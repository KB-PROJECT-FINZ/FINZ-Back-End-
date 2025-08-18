package org.scoula.util.chatbot;

import lombok.extern.log4j.Log4j2;
import org.scoula.domain.chatbot.dto.RecommendationStock;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ProfileStockFilter {

    // 공개 API: 성향별 k개 선택
    public static List<RecommendationStock> selectByRiskType(String riskType, List<RecommendationStock> stocks, int k) {
        // 베이스 하드필터
        var base = stocks.stream()
                .filter(s -> pos(s.getPer()) && pos(s.getPbr()) && pos(s.getRoe())
                        && pos(s.getVolume()) && pos(s.getPrice()))
                .toList();
        if (base.isEmpty()) return List.of();

        // 점수화
        var scored = base.stream()
                .map(s -> new Scored(s, score(riskType, s)))
                .sorted(Comparator.comparingDouble((Scored x) -> x.score).reversed())
                .toList();

        // 상위 풀(최소 30개 또는 k*10)
        int pool = Math.min(Math.max(k * 10, 30), scored.size());
        var poolList = scored.subList(0, pool);

        // 다양화 (회전율 밴드 + 이름 유사도 억제)
        var diversified = diversify(poolList, k, 0.7);

        // 부족 시 상위권에서 랜덤 보충
        if (diversified.size() < k) {
            var remain = k - diversified.size();
            var chosen = diversified.stream().map(s -> s.stock.getCode()).collect(Collectors.toSet());
            var rest = new ArrayList<>(poolList);
            rest.removeIf(s -> chosen.contains(s.stock.getCode()));
            Collections.shuffle(rest);
            for (var r : rest) { diversified.add(r); if (diversified.size() >= k) break; }
        }
        return diversified.stream().map(s -> s.stock).toList();
    }

    // 성향별 스코어
    private static double score(String risk, RecommendationStock s) {
        double per = safe(s.getPer());
        double pbr = safe(s.getPbr());
        double roe = safe(s.getRoe());
        double vol = safe(s.getVolume());
        double trn = safe(s.getTurnRate());
        double price = safe(s.getPrice());
        double h52 = safe(s.getHigh52w());
        double l52 = safe(s.getLow52w());

        // 정규화
        double perScore  = clamp(1.0 - norm(per, 5, 40), 0, 1);
        double pbrScore  = clamp(1.0 - norm(pbr, 0.5, 5), 0, 1);
        double roeScore  = clamp(norm(roe, 5, 30), 0, 1);
        double volScore  = clamp(norm(log1p(vol), 10, 20), 0, 1);
        double trnScore  = clamp(norm(trn, 0.2, 3.0), 0, 1);

        // 52주 모멘텀 (저가~고가 대비 현재 위치)
        double mom = 0.0;
        if (h52 > l52 + 1e-9 && price > 0) {
            mom = clamp((price - l52) / (h52 - l52), 0, 1);
        }
        double momScore = mom;

        // 성향별 가중치
        double wPer, wPbr, wRoe, wVol, wTurn, wMom;
        switch (risk) {
            case "AGR": case "DTA": case "THE": // 공격/단타/테마
                wPer=0.08; wPbr=0.07; wRoe=0.18; wVol=0.27; wTurn=0.25; wMom=0.15; break;
            case "VAL": // 가치
                wPer=0.35; wPbr=0.25; wRoe=0.25; wVol=0.07; wTurn=0.03; wMom=0.05; break;
            case "TEC": // 기술적
                wPer=0.10; wPbr=0.08; wRoe=0.20; wVol=0.25; wTurn=0.22; wMom=0.15; break;
            case "IND": case "CSD": // 안정/인덱스
                wPer=0.28; wPbr=0.22; wRoe=0.30; wVol=0.08; wTurn=0.02; wMom=0.10; break;
            case "INF": case "SYS": // 정보/시스템
                wPer=0.18; wPbr=0.12; wRoe=0.25; wVol=0.18; wTurn=0.17; wMom=0.10; break;
            default:
                wPer=0.2; wPbr=0.2; wRoe=0.3; wVol=0.15; wTurn=0.10; wMom=0.05;
        }
        return wPer*perScore + wPbr*pbrScore + wRoe*roeScore + wVol*volScore + wTurn*trnScore + wMom*momScore;
    }

    // 다양화: 회전율 밴드(저/중/고) 각 1개 시드 + MMR
    private static List<Scored> diversify(List<Scored> pool, int k, double lambda) {
        var picked = new ArrayList<Scored>();

        // 밴드 시드
        pickBandSeed(picked, pool, 0); // low
        pickBandSeed(picked, pool, 1); // mid
        pickBandSeed(picked, pool, 2); // high

        // MMR로 나머지 채우기 (이름 유사도 억제)
        while (picked.size() < k) {
            Scored best = null; double bestMMR = -1;
            for (var cand : pool) {
                if (contains(picked, cand.stock.getCode())) continue;
                double rel = cand.score;
                double simMax = 0.0;
                for (var p : picked) simMax = Math.max(simMax, nameSim(cand.stock, p.stock));
                double mmr = lambda*rel - (1.0 - lambda)*simMax;
                if (mmr > bestMMR) { bestMMR = mmr; best = cand; }
            }
            if (best == null) break;
            picked.add(best);
        }
        // 부족하면 점수순 채움
        if (picked.size() < k) {
            for (var c : pool) { if (!contains(picked, c.stock.getCode())) picked.add(c); if (picked.size()>=k) break; }
        }
        return picked.size() > k ? picked.subList(0, k) : picked;
    }

    // === helpers ===
    private static void pickBandSeed(List<Scored> out, List<Scored> pool, int band) {
        for (var s : pool) { if (bandTurn(s.stock) == band && !contains(out, s.stock.getCode())) { out.add(s); break; } }
    }
    private static boolean contains(List<Scored> list, String code){ return list.stream().anyMatch(x -> x.stock.getCode().equals(code)); }
    private static int bandTurn(RecommendationStock s){ double t = safe(s.getTurnRate()); if (t < 0.5) return 0; if (t < 1.5) return 1; return 2; }
    private static double nameSim(RecommendationStock a, RecommendationStock b) {
        Set<String> A = tokens(a.getName());
        Set<String> B = tokens(b.getName());
        return jaccard(A, B);
    }
    private static Set<String> tokens(String s){ if (s==null) return Set.of(); return Arrays.stream(s.toLowerCase().split("[^a-z0-9가-힣]+")).filter(t->t.length()>=2).collect(Collectors.toSet()); }
    private static double jaccard(Set<String> a, Set<String> b){ if (a.isEmpty() && b.isEmpty()) return 0; var inter=new HashSet<>(a); inter.retainAll(b); var uni=new HashSet<>(a); uni.addAll(b); return (double)inter.size()/uni.size(); }

    private static boolean pos(Double v){ return v!=null && !v.isNaN() && !v.isInfinite() && v>0.0; }
    private static double safe(Double v){ return v==null?0.0:v; }
    private static double norm(double x,double lo,double hi){ return (x-lo)/Math.max(1e-9,(hi-lo)); }
    private static double clamp(double x,double lo,double hi){ return Math.max(lo, Math.min(hi, x)); }
    private static double log1p(double v){ return Math.log1p(Math.max(0.0, v)); }

    private static class Scored {
        final RecommendationStock stock; final double score;
        Scored(RecommendationStock s, double sc){ this.stock=s; this.score=sc; }
    }
}

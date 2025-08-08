package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.domain.ranking.MyDistributionDto;
import org.scoula.domain.ranking.PopularStockDto;
import org.scoula.domain.ranking.TraitStockDto;
import org.scoula.mapper.ranking.AnalysisMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisMapper analysisMapper;

    @Override
    public List<TraitStockDto> getTraitStocks(Long userId) {
        return analysisMapper.findTraitStocks(userId);
    }

    @Override
    public List<MyDistributionDto> getMyDistribution(Long userId) {
        return analysisMapper.findMyDistribution(userId);
    }

    @Override
    public List<PopularStockDto> getPopularStocksByTrait(String traitGroup) {
        String normalizedGroup = traitGroup;
        if ("ANALYTICAL".equalsIgnoreCase(traitGroup) || "EMOTIONAL".equalsIgnoreCase(traitGroup)) {
            normalizedGroup = "SPECIAL";
        }
        return analysisMapper.findPopularStocksByTrait(normalizedGroup);
    }

    @Override
    public void saveMyStockDistribution(int userId, List<MyDistributionDto> distributions) {
        log.info("✅ [saveMyStockDistribution] userId = {}", userId);

        for (MyDistributionDto dist : distributions) {
            log.info("📦 처리 중인 Distribution: {}", dist);

            try {
                int updated = analysisMapper.updateDistribution(userId, dist);
                log.info("🛠 updateDistribution 결과: updated = {}", updated);

                if (updated == 0) {
                    log.info("🚨 update 실패 → insertDistribution 시도: stockCode = {}", dist.getStockCode());
                    analysisMapper.insertDistribution(userId, dist);
                    log.info("✅ insertDistribution 완료: stockCode = {}", dist.getStockCode());
                }

            } catch (Exception e) {
                log.error("❌ 예외 발생! stockCode = {}, message = {}", dist.getStockCode(), e.getMessage(), e);
                throw e; // 롤백을 위해 re-throw
            }
        }
    }
}

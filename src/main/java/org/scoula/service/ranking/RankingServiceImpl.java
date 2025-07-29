package org.scoula.service.ranking;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.ranking.*;
import org.scoula.mapper.ranking.RankingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    @Override
    public MyRankingDto getMyRanking(Long userId) {
        MyRankingDto dto = new MyRankingDto();
        dto.setUserId(userId);
        dto.setGainRate(BigDecimal.valueOf(12.75));
        dto.setRanking(17);
        dto.setTopPercent(5.3); // 상위 5.3%
        return dto;
    }

    @Override
    public List<PopularStockDto> getTop5Stocks() {
        return List.of(
                createStock("005930", "삼성전자", 3050, 1),
                createStock("000660", "SK하이닉스", 2412, 2),
                createStock("035720", "카카오", 1888, 3),
                createStock("035420", "NAVER", 1742, 4),
                createStock("051910", "LG화학", 1621, 5)
        );
    }

    private PopularStockDto createStock(String code, String name, int txCount, int rank) {
        PopularStockDto dto = new PopularStockDto();
        dto.setStockCode(code);
        dto.setStockName(name);
        dto.setTransactionCount(txCount);
        dto.setRanking(rank);
        return dto;
    }

    @Override
    public List<RankingByTraitGroupDto> getWeeklyRanking() {
        List<RankingByTraitGroupDto> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            RankingByTraitGroupDto dto = new RankingByTraitGroupDto();
            dto.setUserId((long) i);
            dto.setTraitGroup(i % 2 == 0 ? "AGGRESSIVE" : "BALANCED");
            dto.setGainRate(BigDecimal.valueOf(20.0 - i));
            dto.setRanking(i);
            list.add(dto);
        }
        return list;
    }

    @Override
    public Map<String, List<RankingByTraitGroupDto>> getGroupedWeeklyRanking() {
        Map<String, List<RankingByTraitGroupDto>> map = new LinkedHashMap<>();

        String[] groups = {"CONSERVATIVE", "BALANCED", "AGGRESSIVE", "ANALYTICAL", "EMOTIONAL"};
        for (String group : groups) {
            List<RankingByTraitGroupDto> list = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                RankingByTraitGroupDto dto = new RankingByTraitGroupDto();
                dto.setUserId((long) (i + 100));
                dto.setTraitGroup(group);
                dto.setGainRate(BigDecimal.valueOf(10.0 + i));
                dto.setRanking(i);
                list.add(dto);
            }
            map.put(group, list);
        }

        return map;
    }
}
//package org.scoula.controller.ranking;
//
//import lombok.RequiredArgsConstructor;
//import org.scoula.service.ranking.AssetHistoryService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/ranking")
//public class RankingTestController {
//
//    private final AssetHistoryService assetHistoryService;
//
//    // 전체 유저 이번주 자산 이력 저장 테스트용 API
//    @PostMapping("/test/save-asset-all-this-week")
//    public ResponseEntity<String> saveAssetHistoryAllThisWeek() {
//        assetHistoryService.saveAssetHistoryForAllUsersThisWeek();
//        return ResponseEntity.ok("이번주 자산 이력 전체 저장 완료");
//    }
//}
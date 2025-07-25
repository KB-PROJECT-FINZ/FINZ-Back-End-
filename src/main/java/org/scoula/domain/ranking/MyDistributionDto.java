package org.scoula.domain.ranking;


import lombok.Data;

@Data
public class MyDistributionDto {
    private String name;          // 종목명
    private double gain;          // 수익률
    private int positionIndex;    // 분포 내 위치 인덱스
    private String positionLabel; // 분포 내 위치 텍스트
    private int distributionBin1; // 분포 구간1
    private int distributionBin2; // 분포 구간2
    private int distributionBin3; // 분포 구간3
    private int distributionBin4; // 분포 구간4
    private int distributionBin5; // 분포 구간5
    private int distributionBin6; // 분포 구간6
    private String color;         // 차트 색상
}

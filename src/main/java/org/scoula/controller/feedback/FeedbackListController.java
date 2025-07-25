package org.scoula.controller.feedback;

import lombok.RequiredArgsConstructor;
import org.scoula.domain.feedback.vo.JournalFeedback;
import org.scoula.service.feedback.JournalFeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackListController {
    private final JournalFeedbackService journalFeedbackService;

    @GetMapping
    public List<JournalFeedback> getAllFeedbacks() {
        return journalFeedbackService.getAllFeedbacks();
    }
}

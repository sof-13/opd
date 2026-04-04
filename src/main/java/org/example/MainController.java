package org.example;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

import java.io.InputStream;
import java.util.*;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/facts")
    public String factsQuestion(@RequestParam(defaultValue = "0") int index,
                                @RequestParam(defaultValue = "true") boolean isNewGame,
                                HttpSession session, Model model) {
        try {
            ClassPathResource resource = new ClassPathResource("static/data/facts.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode factsArray = mapper.readTree(inputStream);

            @SuppressWarnings("unchecked")
            List<JsonNode> shuffledFacts = (List<JsonNode>) session.getAttribute("shuffledFacts");

            if (shuffledFacts == null || isNewGame) {
                shuffledFacts = new ArrayList<>();
                for (JsonNode fact : factsArray) { shuffledFacts.add(fact); }
                Collections.shuffle(shuffledFacts);
                session.setAttribute("shuffledFacts", shuffledFacts);
            }

            int totalQuestions = shuffledFacts.size();
            int currentIndex = index >= totalQuestions ? 0 : index;
            JsonNode currentFact = shuffledFacts.get(currentIndex);

            model.addAttribute("totalQuestions", totalQuestions);
            model.addAttribute("currentQuestion", currentIndex + 1);
            model.addAttribute("questionText", currentFact.get("question").asText());
            model.addAttribute("imageUrl", currentFact.get("imageUrl").asText());
            model.addAttribute("isMyth", currentFact.get("isMyth").asBoolean());
            model.addAttribute("description", currentFact.get("description").asText());
            model.addAttribute("nextIndex", currentIndex + 1);
            model.addAttribute("isLastQuestion", currentIndex == totalQuestions - 1);
            model.addAttribute("showExitButton", currentIndex < totalQuestions - 1);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("questionText", "Ошибка загрузки вопроса");
        }
        return "facts-question";
    }

    @GetMapping("/facts/early-exit")
    public String factsEarlyExit(HttpSession session) {
        session.removeAttribute("shuffledFacts");
        return "facts-early-exit";
    }

    @GetMapping("/facts/complete")
    public String factsComplete(HttpSession session) {
        session.removeAttribute("shuffledFacts");
        return "facts-complete";
    }

    @GetMapping("/quiz")
    public String quizStart(HttpSession session) {
        session.removeAttribute("quizScore");
        session.removeAttribute("quizQuestionIndex");
        session.removeAttribute("lastSelectedOption");
        session.removeAttribute("lastQuestionIndex");
        session.removeAttribute("shuffledOptions");
        session.removeAttribute("shuffledQuestions");
        session.setAttribute("quizScore", 0);
        session.setAttribute("quizQuestionIndex", 0);
        return "redirect:/quiz/question";
    }

    @GetMapping("/quiz/question")
    public String quizQuestion(HttpSession session, Model model) {
        try {
            ClassPathResource resource = new ClassPathResource("static/data/quiz.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode quizArray = mapper.readTree(inputStream);

            @SuppressWarnings("unchecked")
            List<JsonNode> shuffledQuestions = (List<JsonNode>) session.getAttribute("shuffledQuestions");

            if (shuffledQuestions == null) {
                shuffledQuestions = new ArrayList<>();
                for (JsonNode q : quizArray) {
                    shuffledQuestions.add(q);
                }
                Collections.shuffle(shuffledQuestions);
                session.setAttribute("shuffledQuestions", shuffledQuestions);
            }

            Integer questionIndex = (Integer) session.getAttribute("quizQuestionIndex");
            if (questionIndex == null) {
                questionIndex = 0;
                session.setAttribute("quizQuestionIndex", 0);
            }

            int totalQuestions = shuffledQuestions.size();
            if (questionIndex >= totalQuestions) {
                return "redirect:/quiz/result";
            }

            JsonNode currentQuestion = shuffledQuestions.get(questionIndex);
            JsonNode options = currentQuestion.get("options");

            @SuppressWarnings("unchecked")
            List<String> shuffledOptions = (List<String>) session.getAttribute("shuffledOptions");

            if (shuffledOptions == null || questionIndex == 0) {
                shuffledOptions = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
                Collections.shuffle(shuffledOptions);
                session.setAttribute("shuffledOptions", shuffledOptions);
            }

            model.addAttribute("questionIndex", questionIndex);
            model.addAttribute("totalQuestions", totalQuestions);
            model.addAttribute("currentQuestion", questionIndex + 1);
            model.addAttribute("problem", currentQuestion.get("problem").asText());

            // Передаём варианты в рандомном порядке
            model.addAttribute("option1Key", shuffledOptions.get(0));
            model.addAttribute("option1Text", options.get(shuffledOptions.get(0)).get("text").asText());
            model.addAttribute("option2Key", shuffledOptions.get(1));
            model.addAttribute("option2Text", options.get(shuffledOptions.get(1)).get("text").asText());
            model.addAttribute("option3Key", shuffledOptions.get(2));
            model.addAttribute("option3Text", options.get(shuffledOptions.get(2)).get("text").asText());
            model.addAttribute("option4Key", shuffledOptions.get(3));
            model.addAttribute("option4Text", options.get(shuffledOptions.get(3)).get("text").asText());

            Integer currentScore = (Integer) session.getAttribute("quizScore");
            model.addAttribute("currentScore", currentScore != null ? currentScore : 0);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("problem", "Ошибка загрузки вопроса");
        }
        return "quiz-question";
    }

    @PostMapping("/quiz/answer")
    public String quizAnswer(@RequestParam String selectedOption,
                             @RequestParam int questionIndex,
                             HttpSession session) {
        session.setAttribute("lastSelectedOption", selectedOption);
        session.setAttribute("lastQuestionIndex", questionIndex);
        return "redirect:/quiz/answer-result";
    }

    @GetMapping("/quiz/answer-result")
    public String quizAnswerResult(HttpSession session, Model model) {
        try {
            Integer questionIndex = (Integer) session.getAttribute("lastQuestionIndex");
            String selectedOption = (String) session.getAttribute("lastSelectedOption");

            if (questionIndex == null || selectedOption == null) {
                return "redirect:/quiz/question";
            }

            @SuppressWarnings("unchecked")
            List<JsonNode> shuffledQuestions = (List<JsonNode>) session.getAttribute("shuffledQuestions");

            if (shuffledQuestions == null) {
                return "redirect:/quiz/question";
            }

            JsonNode currentQuestion = shuffledQuestions.get(questionIndex);
            JsonNode selectedData = currentQuestion.get("options").get(selectedOption);

            int points = selectedData.get("points").asInt();
            String explanation = selectedData.get("explanation").asText();
            String optionText = selectedData.get("text").asText();

            Integer currentScore = (Integer) session.getAttribute("quizScore");
            int newScore = (currentScore != null ? currentScore : 0) + points;
            session.setAttribute("quizScore", newScore);

            model.addAttribute("points", points);
            model.addAttribute("explanation", explanation);
            model.addAttribute("optionText", optionText);
            model.addAttribute("newScore", newScore);
            model.addAttribute("questionIndex", questionIndex);
            model.addAttribute("totalQuestions", shuffledQuestions.size());

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/quiz/question";
        }
        return "quiz-answer-result";
    }

    @GetMapping("/quiz/next")
    public String quizNext(HttpSession session) {
        Integer questionIndex = (Integer) session.getAttribute("lastQuestionIndex");
        if (questionIndex == null) { questionIndex = 0; }
        questionIndex++;
        session.setAttribute("quizQuestionIndex", questionIndex);

        List<String> shuffledOptions = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        Collections.shuffle(shuffledOptions);
        session.setAttribute("shuffledOptions", shuffledOptions);

        session.removeAttribute("lastSelectedOption");
        session.removeAttribute("lastQuestionIndex");
        return "redirect:/quiz/question";
    }

    @GetMapping("/quiz/exit")
    public String quizExit() {
        return "quiz-exit";
    }

    @GetMapping("/quiz/result")
    public String quizResult(HttpSession session, Model model) {
        Integer totalScore = (Integer) session.getAttribute("quizScore");
        if (totalScore == null) { totalScore = 0; }

        String engineerType, description, advice, image;

        if (totalScore <= 45) {
            engineerType = "Техник / Инженер-техник";
            description = "Ты решаешь задачи просто и эффективно, выбираешь понятные и проверенные решения. Это отличная база для старта! Техники и инженеры-техники — это специалисты, которые обслуживают оборудование, устраняют неисправности и обеспечивают бесперебойную работу систем.";
            advice = "Попробуй кружки по робототехнике или курсы по основам проектирования — это поможет перейти на следующий уровень.";
            image = "/images/quiz/result-technician.jpg";
        } else if (totalScore <= 65) {
            engineerType = "Инженер-конструктор / Инженер КИПиА";
            description = "Ты не просто делаешь, а думаешь: сравниваешь варианты, ищешь причины, учитываешь детали. У тебя сильный потенциал! Инженеры-конструкторы разрабатывают чертежи и схемы, а инженеры КИПиА отвечают за точность и надёжность систем.";
            advice = "Развивай навыки системного мышления и работы с данными — попробуй задачи на оптимизацию или программирование.";
            image = "/images/quiz/result-designer.jpg";
        } else if (totalScore <= 85) {
            engineerType = "Инженер-проектировщик / Инженер-разработчик";
            description = "Ты видишь задачу целиком: от реализации до профилактики и развития процесса. Инженеры-проектировщики создают комплексные решения, а инженеры-разработчики работают над новыми продуктами и технологиями.";
            advice = "Участвуй в проектных конкурсах и олимпиадах — твой уровень мышления уже позволяет решать комплексные задачи.";
            image = "/images/quiz/result-developer.jpg";
        } else {
            engineerType = "Главный инженер проекта (ГИП) / Технический директор";
            description = "Ты не просто решаешь задачи — ты создаёшь условия, чтобы они не возникали. Ты думаешь о команде, будущем и масштабе. Главный инженер проекта руководит всеми инженерными разделами, а технический директор определяет стратегию развития технологий.";
            advice = "Готовься к вузу, ищи стажировки в технологических компаниях и не бойся брать на себя ответственность за проекты.";
            image = "/images/quiz/result-director.jpg";
        }

        model.addAttribute("totalScore", totalScore);
        model.addAttribute("engineerType", engineerType);
        model.addAttribute("description", description);
        model.addAttribute("advice", advice);
        model.addAttribute("image", image);

        session.removeAttribute("quizScore");
        session.removeAttribute("quizQuestionIndex");
        session.removeAttribute("shuffledOptions");
        session.removeAttribute("shuffledQuestions");

        return "quiz-result";
    }
}
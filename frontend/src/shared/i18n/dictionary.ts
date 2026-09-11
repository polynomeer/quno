/** 다국어 지원 대상은 핵심 플로우 4곳(홈/로그인/회원가입/질문 작성)뿐이다 — 나머지 화면은
 * 여전히 한국어 하드코딩이다(ADR-0053). `ko`가 원본이자 타입의 기준이고, `en`은 `Dictionary`를
 * 만족해야 해 키 하나라도 빠지면 컴파일 에러가 난다. */
export const ko = {
  home: {
    guestTitle: "개발자의 지식이 모두의 성장이 됩니다",
    guestSubtitle: "로그인하면 인기 질문과 Ward 업데이트를 볼 수 있습니다.",
    popularQuestions: "인기 질문",
    followingTagsFeed: "관심 태그 피드",
    resolvedToday: "오늘 해결된 질문",
    reopenedKnowledge: "재활성화된 지식",
    flow: "Quno Flow",
    noQuestionsYet: "아직 질문이 없습니다.",
  },
  login: {
    title: "로그인",
    submit: "로그인",
    submitting: "로그인 중...",
    noAccount: "계정이 없으신가요?",
    signUpLink: "회원가입",
    failed: "로그인에 실패했습니다.",
  },
  signup: {
    title: "회원가입",
    submit: "회원가입",
    submitting: "가입 중...",
    haveAccount: "이미 계정이 있으신가요?",
    loginLink: "로그인",
    failed: "회원가입에 실패했습니다.",
  },
  ask: {
    heading: "Ask a question",
    bodyPlaceholder: "본문을 작성하세요 (Markdown 지원)",
    environmentLabel: "Environment (선택)",
    environmentPlaceholder: "예: Spring Boot 4.0.8, Kotlin 2.1",
    logsLabel: "Logs (선택)",
    logsPlaceholder: "에러 로그를 붙여넣으세요",
    tagsLabel: "Tags",
    submitting: "등록 중...",
    submit: "Post",
    failed: "질문을 등록하지 못했습니다.",
    similarQuestions: "Similar Questions",
    noSimilarQuestions: "비슷한 질문이 없습니다.",
    similarQuestionsHint: "제목을 3자 이상 입력하면 비슷한 질문을 보여줍니다.",
    titleRequired: "제목을 입력하세요",
    titleTooLong: "제목은 300자를 넘을 수 없습니다",
    bodyRequired: "본문을 입력하세요",
  },
  languageSwitcher: {
    label: "Language",
  },
};

export type Dictionary = typeof ko;

export const en: Dictionary = {
  home: {
    guestTitle: "Developer knowledge becomes everyone's growth",
    guestSubtitle: "Log in to see popular questions and Ward updates.",
    popularQuestions: "Popular Questions",
    followingTagsFeed: "Followed Tags Feed",
    resolvedToday: "Resolved Today",
    reopenedKnowledge: "Reactivated Knowledge",
    flow: "Quno Flow",
    noQuestionsYet: "No questions yet.",
  },
  login: {
    title: "Log in",
    submit: "Log in",
    submitting: "Logging in...",
    noAccount: "Don't have an account?",
    signUpLink: "Sign up",
    failed: "Login failed.",
  },
  signup: {
    title: "Sign up",
    submit: "Sign up",
    submitting: "Signing up...",
    haveAccount: "Already have an account?",
    loginLink: "Log in",
    failed: "Sign up failed.",
  },
  ask: {
    heading: "Ask a question",
    bodyPlaceholder: "Write the body (Markdown supported)",
    environmentLabel: "Environment (optional)",
    environmentPlaceholder: "e.g. Spring Boot 4.0.8, Kotlin 2.1",
    logsLabel: "Logs (optional)",
    logsPlaceholder: "Paste the error log",
    tagsLabel: "Tags",
    submitting: "Posting...",
    submit: "Post",
    failed: "Failed to post the question.",
    similarQuestions: "Similar Questions",
    noSimilarQuestions: "No similar questions found.",
    similarQuestionsHint: "Type 3+ characters in the title to see similar questions.",
    titleRequired: "Please enter a title",
    titleTooLong: "Title must be 300 characters or fewer",
    bodyRequired: "Please enter a body",
  },
  languageSwitcher: {
    label: "Language",
  },
};

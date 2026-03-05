import { authFetch } from "/js/common/authFetch.js";
console.log("[mypage.js] loaded");



  /* ===============================
     매너 온도 색상
  =============================== */
  function getMannerTextColor(percent) {
    const p = Math.max(0, Math.min(100, percent)) / 100;
    const start = { r: 50, g: 90, b: 210 };
    const end   = { r: 255, g: 0, b: 0 };
    const r = Math.round(start.r + (end.r - start.r) * p);
    const g = Math.round(start.g + (end.g - start.g) * p);
    const b = Math.round(start.b + (end.b - start.b) * p);
    return `rgb(${r}, ${g}, ${b})`;
  }


document.addEventListener("DOMContentLoaded", async () => {

  /* ===============================
     DOM
  =============================== */
  const nicknameEl = document.getElementById("nickname");
  const ageEl = document.getElementById("age");
  const introEl = document.getElementById("intro");

  const nativeFlagEl = document.getElementById("nativeLang");
  const studyFlagEl = document.getElementById("studyLang");
  const levelTextEl = document.getElementById("levelLanguage");

  const mannerEl = document.getElementById("manner");
  const mannerFillEl = document.querySelector(".manner-fill");

  const profileImageEl = document.getElementById("profileImage");
  const imageInput = document.getElementById("profileImageInput");

  const modal = document.getElementById("profileModal");
  const openModalBtn = document.getElementById("openProfileModal");
  const closeModalBtn = document.getElementById("closeProfileModal");
  const profileForm = document.getElementById("profileForm");

  const introTextarea = profileForm.querySelector('textarea[name="intro"]');
  const levelSelect = profileForm.querySelector('select[name="levelLanguage"]');


   function setMannerTemp(temp) {
    const percent = Math.max(0, Math.min(100, temp));
    mannerEl.textContent = `${temp.toFixed(1)}°C`;
    mannerFillEl.style.width = `${percent}%`;
    mannerEl.style.color = getMannerTextColor(percent);
  }

  /* ===============================
     마이페이지 정보 로드
  =============================== */
  try {
const res = await authFetch(`${CONTEXT_PATH}api/members/me`);
const result = await res.json();
if (result.status !== "SUCCESS") return;

const user = result.data;

const myUserId = user.id;
    loadMyActivity(myUserId);

    //  게시글 개수 로드
const countRes = await authFetch(`${CONTEXT_PATH}api/members/me/post-count`);
if (countRes.ok) {
  const countResult = await countRes.json();
  document.getElementById("postCount").textContent =
    countResult.data.toLocaleString();
}
    

    nicknameEl.textContent = user.nickname;
    ageEl.textContent = `(${user.age})`;
    introEl.textContent = user.intro ?? "자기소개를 작성해주세요.";

    const flagMap = { KOREAN: "🇰🇷", JAPANESE: "🇯🇵" };
    nativeFlagEl.textContent = flagMap[user.nativeLanguage] ?? "❓";
    studyFlagEl.textContent = flagMap[user.studyLanguage] ?? "❓";

    levelTextEl.textContent = user.levelLanguage ?? "BEGINNER";
    setMannerTemp(user.manner ?? 36.5);

    const imagePath =
      user.profileImagePath && user.profileImageName
        ? `${user.profileImagePath}/${user.profileImageName}`
        : "/images/profile/default.png";
    profileImageEl.src = imagePath;

    introTextarea.value = user.intro ?? "";
    levelSelect.value = user.levelLanguage ?? "";

  } catch (e) {
    console.error(e);
  }

  /* ===============================
     관심사 / 좋아요 로드
  =============================== */
  await loadInterestChips();
  await loadLikedMeList();
  await loadProfileViewList(); 
  await loadRecommendList();
  await loadRecentChats();

  window.addEventListener("interest:updated", loadInterestChips);

  /* ===============================
     프로필 이미지 변경
  =============================== */
  imageInput.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("image", file);

    const res = await authFetch(
      `${CONTEXT_PATH}api/members/me/profile-image`,
      { method: "PUT", body: formData }
    );

    if (res.ok) {
      profileImageEl.src = URL.createObjectURL(file);
    }
  });

  /* ===============================
     모달 제어
  =============================== */
  openModalBtn.addEventListener("click", () => modal.classList.remove("hidden"));
  closeModalBtn.addEventListener("click", () => modal.classList.add("hidden"));

  /* ===============================
     프로필 수정
  =============================== */
  profileForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const res = await authFetch(
      `${CONTEXT_PATH}api/members/me/profile`,
      { method: "PUT", body: new FormData(profileForm) }
    );

    if (res.ok) {
      introEl.textContent = introTextarea.value || "자기소개를 작성해 주세요.";
      levelTextEl.textContent = levelSelect.value;
      modal.classList.add("hidden");
      customAlert("프로필 수정 완료");
    }
  });
});

/* ===============================
   관심사 로드
=============================== */
async function loadInterestChips() {
  try {
    const res = await authFetch(`${CONTEXT_PATH}api/members/me/interests`);
    if (!res.ok) return;

    const result = await res.json();
    renderInterestChips(result.data ?? []);
  } catch (e) {
    console.error(e);
  }
}




function renderInterestChips(interests) {
  const wrap = document.getElementById("interestChips");
  if (!wrap) return;

  wrap.innerHTML = "";

  if (interests.length === 0) {
    wrap.innerHTML = `<span class="chip empty">관심사 없음</span>`;
    return;
  }

  interests.forEach(item => {
    const chip = document.createElement("span");
    chip.className = "chip";
    chip.textContent = convertInterestToLabel(item);
    wrap.appendChild(chip);
  });
}

/* ===============================
   나를 좋아요한 사람
=============================== */
async function loadLikedMeList() {
  const wrap = document.getElementById("likedMeList");
  const likeCountEl = document.getElementById("likeCount");

  if (!wrap || !likeCountEl) return;

  try {
    const res = await authFetch(`${CONTEXT_PATH}api/reactions/liked-me`);
    if (!res.ok) {
      wrap.innerHTML = `<p class="muted">불러오기 실패</p>`;
      return;
    }

    const result = await res.json();
    const list = result.data ?? [];

    likeCountEl.textContent = list.length.toLocaleString();
    wrap.innerHTML = "";

    if (list.length === 0) {
      wrap.innerHTML = `<p class="muted">아직 좋아요가 없습니다.</p>`;
      return;
    }

    list.forEach(user => {

      const imagePath =
        user.profileImagePath && user.profileImageName
          ? `${user.profileImagePath}/${user.profileImageName}`
          : "/images/profile/default.png";

      const item = document.createElement("div");
      item.className = "viewer-item";

     item.innerHTML = `
  <img src="${imagePath}" class="viewer-avatar">

  <span class="viewer-name">${user.nickname}</span>

  <span class="viewer-meta">
    ${user.age ?? ""} · 
    ${getLanguageFlag(user.nativeLanguage)} →
    ${getLanguageFlag(user.studyLanguage)}
  </span>

<button class="btn-view" data-member-id="${user.memberId}">
  프로필
</button>
`;

      wrap.appendChild(item);
    });

  } catch (e) {
    console.error(e);
    wrap.innerHTML = `<p class="muted">오류 발생</p>`;
  }
}


/* ===============================
   시간 포맷
=============================== */
function formatTime(isoString) {
  if (!isoString) return "";
  const diff = (Date.now() - new Date(isoString)) / 1000;
  if (diff < 60) return "방금 전";
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  return `${Math.floor(diff / 86400)}일 전`;
}

/* ===============================
   프로필 이동
=============================== */
document.addEventListener("click", (e) => {
  const btn = e.target.closest(".btn-view");
  if (!btn) return;
 e.stopPropagation();
  const memberId = btn.dataset.memberId;
  if (memberId) {
    location.href = `${CONTEXT_PATH}member/userPage/${memberId}`;
  }
});
/* ===============================
   enum → 한글
=============================== */
const INTEREST_DETAIL_LABEL = {
  MOVIE: "영화",
  DRAMA: "드라마",
  MUSIC: "음악",
  EXHIBITION: "전시·미술관",
  PERFORMANCE: "공연·연극",
  PHOTO: "사진",
  GAME: "게임",
  BOARD_GAME: "보드게임",
  DIY: "DIY·만들기",
  COLLECT: "수집",
  FITNESS: "헬스·피트니스",
  RUNNING: "러닝·조깅",
  YOGA: "요가·필라테스",
  BALL_SPORTS: "구기 스포츠",
  HIKING: "등산·하이킹",
  DOMESTIC_TRAVEL: "국내 여행",
  OVERSEAS_TRAVEL: "해외 여행",
  BACKPACKING: "배낭여행",
  FOOD_TRIP: "맛집 탐방",
  LOCAL_TOUR: "지역 산책·로컬 투어",
  COOKING: "요리",
  BAKING: "베이킹",
  CAFE: "카페 투어",
  ALCOHOL: "술·와인",
  GOURMET: "미식 탐방",
  LANGUAGE_STUDY: "언어 학습",
  CERTIFICATE: "자격증 준비",
  READING: "독서",
  STUDY_GROUP: "스터디 모임",
  CAREER: "커리어 개발",
  PROGRAMMING: "프로그래밍",
  WEB_APP: "웹·앱 개발",
  GAME_DEV: "게임 개발",
  AI_DATA: "AI·데이터",
  IT_TREND: "IT 트렌드",
  DAILY: "일상 공유",
  PET: "반려동물",
  FASHION: "패션",
  INTERIOR: "인테리어",
  WELLNESS: "건강·웰빙"
};

function convertInterestToLabel(item) {
  return INTEREST_DETAIL_LABEL[item.interestDetail]
      ?? INTEREST_DETAIL_LABEL[item.interest]
      ?? "알 수 없음";
}



/* ===============================
   추천 친구 로드
=============================== */
async function loadRecommendList() {
  const wrap = document.getElementById("recommendGrid");
  if (!wrap) return;

  try {
    const res = await authFetch(`${CONTEXT_PATH}api/recommend`);
    if (!res.ok) {
      wrap.innerHTML = `<p class="muted">추천 불러오기 실패</p>`;
      return;
    }

    const list = await res.json();
    wrap.innerHTML = "";

    if (!list || list.length === 0) {
      wrap.innerHTML = `<p class="muted">추천 결과가 없습니다.</p>`;
      return;
    }

    list.slice(0, 4).forEach(user => {

      /* 매칭 점수 */
      const matchPoint = user.matchPoint ?? 0;
      const percent = Math.max(0, Math.min(100, matchPoint)) / 100;

      /* 🔵 시작색 (파랑) */
      const start = { r: 59, g: 130, b: 246 };   // #3b82f6
      /* 🔴 끝색 (빨강) */
      const end   = { r: 239, g: 68,  b: 68 };   // #ef4444

      const r = Math.round(start.r + (end.r - start.r) * percent);
      const g = Math.round(start.g + (end.g - start.g) * percent);
      const b = Math.round(start.b + (end.b - start.b) * percent);

      const matchColor = `rgb(${r}, ${g}, ${b})`;

      const imagePath =
        user.profileImagePath && user.profileImageName
          ? `${user.profileImagePath}/${user.profileImageName}`
          : "/images/profile/default.png";

      const interests = user.interests ?? [];
      const visibleInterests = interests.slice(0, 3);

      let interestsHtml = visibleInterests
        .map(type => `<span class="tag">${convertInterestType(type)}</span>`)
        .join("");

      if (interests.length > 3) {
        interestsHtml += `<span class="tag more">+${interests.length - 3}</span>`;
      }

      const item = document.createElement("article");
      item.className = "reco";
      item.dataset.userId = user.id;

      item.innerHTML = `
        <div class="reco-top">
          <div class="reco-avatar">
            <img src="${imagePath}"
                 style="width:100%; height:100%; object-fit:cover; border-radius:50%;">
          </div>

          <div class="reco-info">
            <strong>${user.nickname}</strong>
            <span class="flag">${getFlag(user.nation)}</span>

            <div class="reco-sub-row">
              <div class="lang">
                ${getLanguageFlag(user.nativeLanguage)}
                →
                ${getLanguageFlag(user.studyLanguage)}
              </div>

              <div class="stars">
                ${renderLevelStars(user.levelLanguage)}
              </div>
            </div>

            <div class="match">
              매칭 
              <span style="color:${matchColor}; font-weight:900;">
                ${matchPoint}%
              </span>
              · ${formatTemp(user.manner)}
            </div>

          </div>
        </div>

        <div class="reco-tags">
          ${interestsHtml || `<span class="tag empty">관심사 없음</span>`}
        </div>
      `;

      wrap.appendChild(item);
    });

  } catch (e) {
    console.error(e);
    wrap.innerHTML = `<p class="muted">오류 발생</p>`;
  }
}

function renderLevelStars(level) {
  const levelMap = {
    BEGINNER: 1,
    INTERMEDIATE: 2,
    ADVANCED: 3,
    NATIVE: 4
  };

  const score = levelMap[level] ?? 0;

  let stars = "";
  for (let i = 0; i < 4; i++) {
    stars += i < score ? "★" : "☆";
  }

  return stars;
}

function formatTemp(temp) {
  const value = temp ?? 36.5;
  const percent = Math.min(100, Math.max(0, value));
  const color = getMannerTextColor(percent);
  return `<span style="color:${color}">${value.toFixed(1)}°C</span>`;
}
function getFlag(nation) {
  const map = {
    KOREA: "🇰🇷",
    JAPAN: "🇯🇵"
  };

  return map[nation] ?? "";
}

function getLanguageFlag(lang) {
  const map = {
    KOREAN: "🇰🇷",
    JAPANESE: "🇯🇵"
  };

  return map[lang] ?? "❓";
}


  
 
function convertInterestType(type) {
  const map = {
    CULTURE: "문화·예술",
    HOBBY: "취미·여가",
    SPORTS: "운동·스포츠",
    TRAVEL: "여행·지역",
    FOOD: "음식·요리",
    STUDY: "학습·자기계발",
    IT: "IT·기술",
    LIFESTYLE: "라이프스타일"
  };

  return map[type] ?? type;
}
async function loadMyActivity(userId) {
  if (!userId) return;

  try {
    const res = await authFetch(`${CONTEXT_PATH}chat/activity/${userId}`);
    if (!res.ok) return;

    const count = await res.json();

    const countEl = document.getElementById("chattingCount");
    if (!countEl) return;

    countEl.textContent = count;

    /* 🔥 단계 표시 */
    const hotLevelEl = document.getElementById("hotLevel"); 
    if (!hotLevelEl) return;

if (count === 0) {
  hotLevelEl.textContent = "지금 대화하면 칼답 가능성! ✨";
  hotLevelEl.className = "hot-level hot-1";
}
else if (count >= 1 && count <= 4) {
  hotLevelEl.textContent = "오늘 대화 분위기가 좋은 분이네요 💬 ";
  hotLevelEl.className = "hot-level hot-2";
}
else if (count >= 5 && count <= 10) {
  hotLevelEl.textContent = "인기멤버에요! 🔥";
  hotLevelEl.className = "hot-level hot-3";
}
else {
  hotLevelEl.textContent = "인플루언서급이에요! 👑 ";
  hotLevelEl.className = "hot-level hot-4";
}

  } catch (err) {
    console.error("❌ 내 활동량 조회 실패:", err);
  }
}
async function loadRecentChats() {
    try {
        const res = await authFetch(`${CONTEXT_PATH}api/chat/rooms`);
        if (!res.ok) return;

        const rooms = await res.json();
        const wrap = document.getElementById("recentChatList");

        if (!wrap) return;

        wrap.innerHTML = "";

        if (!rooms || rooms.length === 0) {
            wrap.innerHTML = `
                <div class="mini-row">
                    <div class="mini-avatar">👤</div>
                    <div class="mini-text">
                        <div class="mini-name">최근 대화가 없습니다</div>
                        <div class="mini-sub">채팅을 시작해보세요</div>
                    </div>
                </div>
            `;
            return;
        }

        const room = rooms[0]; // 1개만

        const row = document.createElement("div");
        row.className = "mini-row";


let profileSrc = "/images/profile/default.png";

if (room.opponentProfileImg && room.opponentProfileImgName) {
  const basePath = room.opponentProfileImg.endsWith("/")
    ? room.opponentProfileImg
    : room.opponentProfileImg + "/";

  profileSrc = basePath + room.opponentProfileImgName;
}

row.innerHTML = `
  <div class="mini-avatar">
    <img src="${profileSrc}" alt="profile">
  </div>
  <div class="mini-text">
    <div class="mini-name">${room.roomName}</div>
    <div class="mini-sub">채팅 계속하기</div>
  </div>
`;

        row.onclick = () => {
            sessionStorage.setItem("openRoomId", room.roomId);
            location.href = `${CONTEXT_PATH}chat`;
        };

        wrap.appendChild(row);

    } catch (e) {
        console.error("최근 대화 로드 실패:", e);
    }
}



/* ===============================
   내 프로필을 본 사람
=============================== */
async function loadProfileViewList() {
  const wrap = document.getElementById("profileViewList");
  if (!wrap) return;

  try {
    const res = await authFetch(`${CONTEXT_PATH}api/profile-views/me`);
    if (!res.ok) {
      wrap.innerHTML = `<p class="muted">불러오기 실패</p>`;
      return;
    }

    const result = await res.json();
    const list = result.data ?? [];

    wrap.innerHTML = "";

    if (list.length === 0) {
      wrap.innerHTML = `<p class="muted">아직 방문자가 없습니다.</p>`;
      return;
    }

    list.forEach(user => {

      const imagePath =
        user.profileImagePath && user.profileImageName
          ? `${user.profileImagePath}/${user.profileImageName}`
          : "/images/profile/default.png";

      const item = document.createElement("div");
      item.className = "viewer-item";

     item.innerHTML = `
  <img src="${imagePath}" class="viewer-avatar">

  <span class="viewer-name">${user.nickname}</span>

  <span class="viewer-meta">
    ${user.age ?? ""} · 
    ${getLanguageFlag(user.nativeLanguage)} →
    ${getLanguageFlag(user.studyLanguage)}
  </span>

<button class="btn-view" data-member-id="${user.memberId}">
  프로필
</button>
`;

    const btn = item.querySelector(".btn-view");
    btn.addEventListener("click", () => {
        location.href = `${CONTEXT_PATH}member/userPage/${user.memberId}`;
    });

      wrap.appendChild(item);
    });

  } catch (e) {
    console.error(e);
    wrap.innerHTML = `<p class="muted">오류 발생</p>`;
  }
}


document.addEventListener("click", async (e) => {
  const card = e.target.closest(".reco");
  if (!card) return;

  const partnerId = card.dataset.userId;
  if (!partnerId) return;

  card.style.pointerEvents = "none";

  try {
    const res = await authFetch(
      `${CONTEXT_PATH}api/chat/rooms/direct/${partnerId}`,
      { method: "POST" }
    );

    if (!res.ok) throw new Error("HTTP " + res.status);

    const data = await res.json();

    sessionStorage.setItem("openRoomId", data.roomId);
    location.href = `${CONTEXT_PATH}chat`;

  } catch (err) {
    console.error(err);
    customAlert("채팅방 생성 실패");
    card.style.pointerEvents = "auto";
  }
});



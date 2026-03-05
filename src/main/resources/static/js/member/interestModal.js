document.addEventListener("DOMContentLoaded", async () => {
  const MAX = 5;

  const modal = document.getElementById("interestModal");
  if (!modal) return;

  const openBtn = document.getElementById("openInterestModal");
  const closeBtn = document.getElementById("closeInterestBtn");
  const saveBtn = document.getElementById("saveInterestBtn");
  const items = document.querySelectorAll(".interest-item");
  const chipArea = document.getElementById("interestChips");

  /* =========================
     모달 열기 / 닫기
     ========================= */

  openBtn?.addEventListener("click", async () => {
    //항상 초기화 후 최신 값 GET
    clearActiveItems();
    await loadUserInterests();
    modal.classList.remove("hidden");
  });

  closeBtn?.addEventListener("click", () => {
    modal.classList.add("hidden");
  });

  /* =========================
     버튼 클릭 처리
     ========================= */

  items.forEach(item => {
    item.addEventListener("click", () => toggleItem(item));
  });

  /* =========================
     저장 버튼
     ========================= */

  saveBtn?.addEventListener("click", async (e) => {
    e.preventDefault();

    //  현재 active 상태만 기준
    const interests = getSelectedInterests();

    if (interests.length === 0) {
      customAlert("관심사를 하나 이상 선택해 주세요.");
      return;
    }

    try {
      //  POST → 서버에서 기존 전부 delete + 새로 insert
      await saveInterests(interests);

      //  화면 상태 완전 재동기화
      clearActiveItems();
      await loadUserInterests();
          window.dispatchEvent(new Event("interest:updated"));

     customAlert("관심사 수정 완료");
      modal.classList.add("hidden");
    } catch (e) {
      console.error(e);
      customAlert("관심사 저장 실패");
    }
  });

  /* =========================
     함수들
     ========================= */

  function toggleItem(item) {
    if (item.classList.contains("active")) {
      item.classList.remove("active");
      return;
    }

    const count = document.querySelectorAll(".interest-item.active").length;
    if (count >= MAX) {
      customAlert("관심사는 최대 5개까지 선택할 수 있어요!");
      return;
    }

    item.classList.add("active");
  }

  //  POST로 보낼 데이터 = 현재 선택 상태
  function getSelectedInterests() {
    return [...document.querySelectorAll(".interest-item.active")]
      .map(btn => ({
        interest: btn.dataset.interest,        // STUDY
        interestDetail: btn.dataset.detail     // LANGUAGE_STUDY
      }));
  }

  function clearActiveItems() {
    document
      .querySelectorAll(".interest-item.active")
      .forEach(el => el.classList.remove("active"));
  }

  async function saveInterests(interests) {
    const res = await fetch(
      CONTEXT_PATH + "api/members/me/interests",
      {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(interests)
      }
    );

    if (!res.ok) throw new Error("Save failed");
  }

  async function loadUserInterests() {
    const res = await fetch(
      CONTEXT_PATH + "api/members/me/interests",
      { credentials: "include" }
    );
    if (!res.ok) return;

    const result = await res.json();
    const interests = result.data ?? [];

    //  모달 버튼 active 처리
    interests.forEach(item => {
      document
        .querySelector(
          `.interest-item[data-interest="${item.interest}"][data-detail="${item.interestDetail}"]`
        )
        ?.classList.add("active");
    });

  }
});

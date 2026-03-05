document.addEventListener("DOMContentLoaded", () => {
  const MAX = 5;
  const items = document.querySelectorAll(".interest-item");
  const saveBtn = document.getElementById("saveInterestBtn");

  items.forEach(item => {
    item.addEventListener("click", () => toggleItem(item));
  });

  saveBtn?.addEventListener("click", async () => {
    const interests = getSelectedInterests();
    if (interests.length === 0) {
      customAlert("관심사를 하나 이상 선택해 주세요.");
      return;
    }

    try {
      await fetch("/api/members/me/interests", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(interests)
      });

      await customAlert("관심사 저장 완료");
      location.href = "/";
    } catch (e) {
      console.error(e);
      customAlert("관심사 저장 실패");
    }
  });

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

  function getSelectedInterests() {
    return [...document.querySelectorAll(".interest-item.active")]
      .map(btn => ({
        interest: btn.dataset.interest,
        interestDetail: btn.dataset.detail
      }));
  }
});

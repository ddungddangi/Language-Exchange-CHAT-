document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("loginForm");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const memberId = form.memberId.value.trim();
    const password = form.password.value.trim();

    const res = await fetch(`${CONTEXT_PATH}api/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include", //  쿠키 받기
      body: JSON.stringify({ memberId, password })
    });

    const result = await res.json();

    if (result.status !== "SUCCESS") {
      await customAlert(result.message);   
      return;
    }

    // 쿠키는 서버가 세팅함 → 그냥 이동
    location.href = `${CONTEXT_PATH}`;
  });
});

document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("signupForm");
  if (!form) return;

  /* =========================
     DOM 요소
  ========================= */
  const imageInput = document.getElementById("imageInput");
  const preview = document.getElementById("preview");
  const plus = document.querySelector(".avatar .plus");

  const memberIdInput = document.getElementById("memberId");
  const nicknameInput = document.getElementById("nickname");
  const studyLanguage = document.getElementById("studyLanguage");

  const tos = document.getElementById("tos");
  const privacy = document.getElementById("privacy");

  /* =========================
     상태 플래그
  ========================= */
  let isMemberIdChecked = false;
  let isNicknameChecked = false;

  /* =========================
     유효성 검사 함수
  ========================= */

  // 아이디: 3~12자, 영문 대소문자 + 숫자
  function validateMemberId(memberId) {
    const regex = /^[A-Za-z0-9]{3,12}$/;
    return regex.test(memberId);
  }

  // 비밀번호: 8~20자, 영문 + 숫자 최소 1개씩
  function validatePassword(password) {
    const regex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/;
    return regex.test(password);
  }

  /* =========================
     아이디 중복 확인
  ========================= */
  document.getElementById("checkIdBtn")?.addEventListener("click", async () => {
    const memberId = memberIdInput.value.trim();

    if (!memberId) {
      await customAlert("아이디를 입력하세요");
      return;
    }

    if (!validateMemberId(memberId)) {
      await customAlert("아이디는 3~12자 영문과 숫자만 가능합니다.");
      return;
    }

    try {
      const res = await fetch(
        `${CONTEXT_PATH}api/members/exists?memberId=${encodeURIComponent(memberId)}`
      );
      const result = await res.json();

      if (res.ok && result.data?.available) {
        await customAlert("사용 가능한 아이디입니다");
        isMemberIdChecked = true;
      } else {
        await customAlert("이미 사용 중인 아이디입니다");
        isMemberIdChecked = false;
      }
    } catch (e) {
      await customAlert("아이디 확인 중 오류가 발생했습니다.");
      isMemberIdChecked = false;
    }
  });

  memberIdInput.addEventListener("input", () => {
    isMemberIdChecked = false;
  });

  /* =========================
     닉네임 중복 확인
  ========================= */
  document.getElementById("checkNicknameBtn")?.addEventListener("click", async () => {
    const nickname = nicknameInput.value.trim();

    if (!nickname) {
      await customAlert("닉네임을 입력하세요");
      return;
    }

    try {
      const res = await fetch(
        `${CONTEXT_PATH}api/members/exists?nickname=${encodeURIComponent(nickname)}`
      );
      const result = await res.json();

      if (res.ok && result.data?.available) {
        await customAlert("사용 가능한 닉네임입니다");
        isNicknameChecked = true;
      } else {
        await customAlert("이미 사용 중인 닉네임입니다");
        isNicknameChecked = false;
      }
    } catch (e) {
      await customAlert("닉네임 확인 중 오류가 발생했습니다.");
      isNicknameChecked = false;
    }
  });

  nicknameInput.addEventListener("input", () => {
    isNicknameChecked = false;
  });

  /* =========================
     비밀번호 실시간 검증 표시
  ========================= */
  form.password.addEventListener("input", () => {
    if (!validatePassword(form.password.value)) {
      form.password.style.border = "2px solid red";
    } else {
      form.password.style.border = "2px solid green";
    }
  });

  /* =========================
     이미지 미리보기
  ========================= */
  imageInput?.addEventListener("change", () => {
    const file = imageInput.files[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      customAlert("이미지 파일만 업로드 가능합니다.");
      imageInput.value = "";
      return;
    }

    preview.src = URL.createObjectURL(file);
    preview.style.display = "block";
    if (plus) plus.style.display = "none";
  });

  /* =========================
     회원가입 submit
  ========================= */
  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const memberId = memberIdInput.value.trim();
    const password = form.password.value;

    /* 아이디 형식 */
    if (!validateMemberId(memberId)) {
      await customAlert("아이디는 3~12자 영문과 숫자만 가능합니다.");
      return;
    }

    /* 비밀번호 형식 */
    if (!validatePassword(password)) {
      await customAlert("비밀번호는 8~20자이며 영문과 숫자를 각각 최소 1개 이상 포함해야 합니다.");
      return;
    }

    /* 비밀번호 확인 */
    if (password !== form.passwordConfirm.value) {
      await customAlert("비밀번호가 일치하지 않습니다.");
      return;
    }

    /* 약관 동의 */
    if (!tos.checked || !privacy.checked) {
      await customAlert("필수 약관에 동의해주세요.");
      return;
    }

    /* 중복 확인 */
    if (!isMemberIdChecked || !isNicknameChecked) {
    await customAlert("아이디와 닉네임 중복 확인을 완료해주세요.");
      return;
    }

    /* 나이 검증 */
    const age = Number(form.age.value);
    if (Number.isNaN(age) || age < 18) {
      await customAlert("회원가입은 18세 이상만 가능합니다.");
      return;
    }

    /* 학습 언어 선택 */
    if (!studyLanguage.value) {
      await customAlert("학습 언어를 선택해주세요.");
      return;
    }

    const signupData = {
      memberId,
      password,
      nickname: nicknameInput.value.trim(),
      gender: form.gender.value,
      age,
      nation: form.nation.value,
      studyLanguage: studyLanguage.value,
      levelLanguage: form.levelLanguage.value
    };

    const formData = new FormData();
    formData.append(
      "data",
      new Blob([JSON.stringify(signupData)], {
        type: "application/json"
      })
    );

    if (imageInput.files.length > 0) {
      formData.append("image", imageInput.files[0]);
    }

    try {
      const res = await fetch(`${CONTEXT_PATH}api/members`, {
        method: "POST",
        body: formData,
        credentials: "include"
      });

      const result = await res.json();

      if (!res.ok || result.status !== "SUCCESS") {
        await customAlert(result.message ?? "회원가입에 실패했습니다.");      
        return;
      }

      await customAlert("회원가입 완료");
      location.href = `${CONTEXT_PATH}member/interest`;
    } catch (e) {
      await customAlert("회원가입 중 오류가 발생했습니다.");
    }
  });
});
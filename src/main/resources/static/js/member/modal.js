const overlay = document.getElementById("customModalOverlay");
const text = document.getElementById("customModalText");
const buttons = document.getElementById("customModalButtons");

/* alert 대체 */
function customAlert(message) {
  return new Promise((resolve) => {
    text.textContent = message;

    buttons.innerHTML = `
      <button class="custom-modal-btn custom-btn-confirm">확인</button>
    `;

    const confirmBtn = buttons.querySelector(".custom-btn-confirm");

    confirmBtn.onclick = () => {
      closeModal();
      resolve();
    };

    openModal();
  });
}

/* confirm 대체 */
function customConfirm(message) {
  return new Promise((resolve) => {
    text.textContent = message;

    buttons.innerHTML = `
      <button class="custom-modal-btn custom-btn-cancel">취소</button>
      <button class="custom-modal-btn custom-btn-confirm">확인</button>
    `;

    const cancelBtn = buttons.querySelector(".custom-btn-cancel");
    const confirmBtn = buttons.querySelector(".custom-btn-confirm");

    cancelBtn.onclick = () => {
      closeModal();
      resolve(false);
    };

    confirmBtn.onclick = () => {
      closeModal();
      resolve(true);
    };

    openModal();
  });
}

function openModal() {
  overlay.classList.add("show");
}

function closeModal() {
  overlay.classList.remove("show");
}
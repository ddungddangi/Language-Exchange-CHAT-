document.addEventListener("DOMContentLoaded", () => {



    const wrapper = document.querySelector(".notice-detail-wrapper");
    const noticeId = wrapper?.dataset.noticeId;

    const editBtn = document.getElementById("editBtn");
    const deleteBtn = document.getElementById("deleteBtn");

    const modal = document.getElementById("editModal");
    const titleInput = document.getElementById("editTitle");
    const contentInput = document.getElementById("editContent");

    const saveBtn = document.getElementById("saveEditBtn");
    const cancelBtn = document.getElementById("cancelEditBtn");

    if (!noticeId) return;

    /* 수정 열기 */
    editBtn?.addEventListener("click", () => {
        fetch(`/customer/notice/${noticeId}/edit`)
            .then(res => res.json())
            .then(data => {
                titleInput.value = data.title;
                contentInput.value = data.content;
                modal.classList.remove("hidden");
            });
    });

    /* 수정 저장 */
    saveBtn?.addEventListener("click", () => {
        fetch(`/customer/notice/${noticeId}/edit`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({
                title: titleInput.value,
                content: contentInput.value
            })
        }).then(() => location.reload());
    });

    /* 수정 취소 */
    cancelBtn?.addEventListener("click", () => {
        modal.classList.add("hidden");
    });

    /* 삭제 */
  deleteBtn?.addEventListener("click", () => {

      showCustomConfirm("정말 삭제하시겠습니까?", () => {

          const form = document.createElement("form");
          form.method = "POST";
          form.action = `/customer/notice/${noticeId}/delete`;

          document.body.appendChild(form);
          form.submit();

      });

  });
});

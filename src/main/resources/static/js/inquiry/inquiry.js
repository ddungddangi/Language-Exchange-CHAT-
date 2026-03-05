document.addEventListener('DOMContentLoaded', () => {

    /* =========================
       파일 업로드 미리보기
    ========================= */
    const fileInput = document.getElementById('fileInput');
    const preview = document.getElementById('preview');
    const previewImg = document.getElementById('previewImg');
    const previewName = document.getElementById('previewName');
    const removeBtn = document.getElementById('removeFile');
    const currentAttachment = document.getElementById('currentAttachment');

    function resetPreview() {
        if (!fileInput) return;
        fileInput.value = '';
        if (previewImg) previewImg.src = '';
        if (previewName) previewName.textContent = '';
        if (preview) preview.classList.add('hidden');
        if (currentAttachment) currentAttachment.style.display = 'block';
    }

    if (fileInput) {
        fileInput.addEventListener('change', () => {
            const file = fileInput.files && fileInput.files[0];
            if (!file) return resetPreview();

            if (currentAttachment) currentAttachment.style.display = 'none';

            const reader = new FileReader();
            reader.onload = (e) => {
                if (previewImg) previewImg.src = e.target.result;
            };
            reader.readAsDataURL(file);

            if (previewName) previewName.textContent = file.name;
            if (preview) preview.classList.remove('hidden');
        });
    }

    if (removeBtn) {
        removeBtn.addEventListener('click', resetPreview);
    }

    /* =========================
       문의/답변 모달 (이벤트 위임)
       - 수정(a.edit-btn) 클릭: 이동
       - 나머지 영역 클릭: 모달 오픈
    ========================= */
    const list = document.querySelector('.inquiry-ul');
    const viewModal = document.getElementById('viewModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalInquiry = document.getElementById('modalInquiryContent');
    const modalAnswer = document.getElementById('modalAnswerContent');
    const modalCloseBtn = document.getElementById('modalCloseBtn');

    function openModalFromItem(item) {
        const status = item.dataset.status || '';
        const title = item.dataset.title || '';
        const content = item.dataset.content || '';
        const answer = item.dataset.answer || '';

        if (modalTitle) modalTitle.textContent = title;
        if (modalInquiry) modalInquiry.textContent = content;

        if (modalAnswer) {
            if (status === 'WAITING') {
                modalAnswer.textContent = '아직 답변 전입니다.';
            } else {
                modalAnswer.textContent = (answer && answer.trim()) ? answer : '답변 내용이 없습니다.';
            }
        }

        if (viewModal) {
            viewModal.classList.remove('hidden');
            viewModal.setAttribute('aria-hidden', 'false');
        }
    }

    function closeModal() {
        if (!viewModal) return;
        viewModal.classList.add('hidden');
        viewModal.setAttribute('aria-hidden', 'true');
    }

    // ✅ 수정 링크는 JS가 건드리지 않게 "위임 + 차단" 처리
    if (list) {
        list.addEventListener('click', (e) => {
            const editLink = e.target.closest('.edit-btn');
            if (editLink) {
                // 링크 이동 그대로 수행
                return;
            }

            const item = e.target.closest('.inquiry-item');
            if (!item) return;

            openModalFromItem(item);
        });

        // 키보드 접근성: Enter/Space로 모달 열기
        list.addEventListener('keydown', (e) => {
            const item = e.target.closest('.inquiry-item');
            if (!item) return;

            if (e.key === 'Enter' || e.key === ' ') {
                // 수정 링크에 포커스면 링크로 이동해야 하므로 제외
                if (e.target.closest('.edit-btn')) return;
                e.preventDefault();
                openModalFromItem(item);
            }
        });
    }

    // 닫기 버튼
    if (modalCloseBtn) {
        modalCloseBtn.addEventListener('click', closeModal);
    }

    // 오버레이 클릭 시 닫기 (카드 클릭은 제외)
    if (viewModal) {
        viewModal.addEventListener('click', (e) => {
            if (e.target === viewModal) closeModal();
        });
    }

    // ESC로 닫기
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeModal();
    });

});

document.addEventListener('DOMContentLoaded', () => {

    const viewModal = document.getElementById('viewModal');
    const closeBtn = document.getElementById('modalCloseBtn');

    function closeModal() {
        if (!viewModal) return;
        viewModal.classList.add('hidden');
        viewModal.setAttribute('aria-hidden', 'true');
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', closeModal);
    }

    // 오버레이 클릭 시 닫기
    if (viewModal) {
        viewModal.addEventListener('click', (e) => {
            if (e.target === viewModal) closeModal();
        });
    }

    // ESC 키
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeModal();
    });

    const deleteForm = document.getElementById("deleteForm");

    if (deleteForm) {
        deleteForm.addEventListener("submit", function(e) {
            e.preventDefault();

            showCustomConfirm("정말 삭제하시겠습니까?", function() {
                deleteForm.submit();
            });
        });
    }

    const editForm = document.querySelector(".edit-form");

    if (editForm) {
        editForm.addEventListener("submit", function(e) {

            e.preventDefault();

            showCustomConfirm("문의 내용을 수정하시겠습니까?", function() {
                editForm.submit();
            });

        });
    }

});


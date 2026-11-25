for (const box of document.querySelectorAll(".source-copy")) {
  const button = box.querySelector("button");
  const code = box.parentElement.querySelector(".source-text code");
  if (!navigator.clipboard || !code) continue;
  const label = button.textContent;
  button.hidden = false;
  button.addEventListener("click", () => {
    navigator.clipboard.writeText(code.textContent).then(
      () => {
        button.textContent = "コピーしました";
        setTimeout(() => {
          button.textContent = label;
        }, 2000);
      },
      () => getSelection().selectAllChildren(code),
    );
  });
}

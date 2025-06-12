$(document).ready(function () {
  const coralSelectElement = $(".workitem-dialog-select");

  const pathToMoveInputWrapper = $("foundation-autocomplete[name='pathToMove']").closest(".coral-Form-fieldwrapper");
  const reasonInputWrapper = $("input[name='rejectReason']").closest(".coral-Form-fieldwrapper");
  const pathToMoveInput = $("input[name='pathToMove']");
  const reasonInput = $("input[name='rejectReason']");
  pathToMoveInputWrapper.hide();
  reasonInput.attr("aria-required", "true");
  if (coralSelectElement.length) {
    const updateInputVisibilityAndMandatory = () => {
      const selectedItem = coralSelectElement[0].selectedItem;

      if (selectedItem) {
        const selectedText = selectedItem.textContent.trim();

        if (selectedText.toLowerCase() === "reject") {
          pathToMoveInputWrapper.hide();
          reasonInputWrapper.show();
          reasonInput.attr("aria-required", "true");
        } else if (selectedText.toLowerCase() === "approve") {
          reasonInputWrapper.hide();
          pathToMoveInputWrapper.show();
          reasonInput.removeAttr("aria-required");
        } else {
        }
      }
    };

    updateInputVisibilityAndMandatory();

    coralSelectElement.on("change", updateInputVisibilityAndMandatory);
  } else {
    console.error("Coral Select Element not found!");
  }
});
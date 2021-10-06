// Open Slack, open up their emoji picker, then copy-paste this into the console

var emojis={}

var picker_list = document.getElementById('emoji-picker-list');

function selectSkinTone(value) {
    let skin_tone_toggle = document.querySelector('.p-emoji_picker_skintone__toggle_btn');
    skin_tone_toggle.click()
    simulateMouseClick(document.querySelector('[data-qa="emoji_skintone_option_' + value + '"]'));
}

function simulateMouseClick(targetNode) {
    function triggerMouseEvent(targetNode, eventType) {
        var clickEvent = document.createEvent('MouseEvents');
        clickEvent.initEvent(eventType, true, true);
        targetNode.dispatchEvent(clickEvent);
    }
    ["mouseover", "mousedown", "mouseup", "click"].forEach(function(eventType) {
        triggerMouseEvent(targetNode, eventType);
    });
}

function isDone () {
    return (picker_list.offsetHeight + picker_list.scrollTop >= picker_list.scrollHeight);
}

function collectMore() {
    document.querySelectorAll('#emoji-picker-list *[data-name]').forEach(
        function(button) {
            emojis[button.getAttribute('data-name')] = button.children[0].getAttribute("src");
        }
    )
};

function download(filename, text) {
  var element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
  element.setAttribute('download', filename);
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}

var skinTone = 1;
picker_list.scrollTo(0, 0);
selectSkinTone(skinTone);

(function myLoop() {
    collectMore();
    if (isDone()) {
        if (skinTone === 4) {
            download("emoji.json", JSON.stringify(emojis, null, 2))
        } else {
            skinTone += 1;
            selectSkinTone(skinTone);
            picker_list.scrollTo(0, 0);
            setTimeout(myLoop, 200);
        }
    } else {
        picker_list.scrollBy(0, 60);
        setTimeout(myLoop, 200);
    }
})();

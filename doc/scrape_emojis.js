// Open Slack in a browser, then copy-paste this into the console (F12)

// It will scroll through the emoji picker 4 times (once for each skin tone),
// and finally download the full list of emoji shortcodes.

// You can parse the actual unicode code points out of the image file names.

// Note: we're no longer using this, instead copy-paste the slack data directly
// from slack's JS sources.

var emojis={}

function selectSkinTone(value) {
    let skin_tone_toggle = document.querySelector('.p-emoji_picker_skintone__toggle_btn');
    skin_tone_toggle.click()
    simulateMouseClick('[data-qa="emoji_skintone_option_' + value + '"]');
}

function simulateMouseClick(selector) {
    let targetNode = document.querySelector(selector);
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
            if (button.children[0]) {
                let href = button.children[0].getAttribute("src");
                if (href.includes("production-standard-emoji-assets")) {
                    emojis[button.getAttribute('data-name')] = href;
                }
            }
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

simulateMouseClick('[data-qa="texty_emoji_button"]');

var picker_list = document.getElementById('emoji-picker-list');
picker_list.scrollTo(0, 0);
selectSkinTone(skinTone);

(function myLoop() {
    collectMore();
    if (isDone()) {
        if (skinTone === 4) {
            download("emoji.json", JSON.stringify(emojis, Object.keys(emojis).sort(), 2))
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

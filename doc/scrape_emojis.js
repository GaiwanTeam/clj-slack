// Open Slack, open up their emoji picker, then copy-paste this into the console

var picker_list = document.getElementById('emoji-picker-list');
picker_list.scrollTo(0, 0);
var emojis={}

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

(function myLoop() {
    collectMore();
    if (isDone()) {
        download("emoji.json", JSON.stringify(emojis, null, 2))
    } else {
        picker_list.scrollBy(0, 60);
        setTimeout(myLoop, 200);
    }
})();

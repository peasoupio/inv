// Origin: https://stackoverflow.com/questions/10406930/how-to-construct-a-websocket-uri-relative-to-the-page-uri

function websocketHost() {
    let loc = window.location, new_uri
    if (loc.protocol === "https:") {
        new_uri = "wss:"
    } else {
        new_uri = "ws:"
    }
    new_uri += "//" + loc.host

    return new_uri
}
Vue.component('install', {
    template: `
<div>
    <div style="position: sticky; top: 6em">
        <div class="buttons is-right">
            <a href="#" class="is-link" @click="goToTop()" style="margin-right: 1em;">
                Go to top
            </a>
            <a href="#" class="is-link" @click="goToEnd()" style="margin-right: 1em;">
                Go to end
            </a>
            <button class="button is-info" :disabled="execution.running" @click="start()" v-bind:class=" { 'is-loading': execution.running }">
                Execute
            </button>

            <button class="button is-danger" :disabled="!execution.running" @click="stop()">
                Stop
            </button>
        </div>
    </div>

    <hr />

    <p class="title is-5">
        Output
        <span class="icon is-small" v-if="loadingMessages">
            <i class="fas fa-spinner fa-pulse"></i>
        </span>
        <p class="subtitle is-6" v-if="!execution.running">Last execution: {{getRelativeTimestamp()}}</p>
    </p>
    <div class="output">
        <div ref="logContainer"></div>
        <div class="anchor"></div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            refreshInterval: {},
            refreshTime: 1000,
            loaded: false,
            loadingMessages: true,
            execution: {},
            messages: []
        }
    },
    methods: {
        clearLog: function(logContainer) {
            // Reset elements
            var child = logContainer.lastElementChild;
            while (child) {
                logContainer.removeChild(child);
                child = logContainer.lastElementChild;
            }
        },
        appendLog: function(logContainer, message) {
            var pre = document.createElement("PRE")
            pre.appendChild(document.createTextNode(message))

            logContainer.appendChild(pre)
        },
        start: function() {
            var vm = this

            if (vm.execution.running)
                return

            vm.follow()

            axios.post(vm.execution.links.start)
        },
        stop: function() {
            var vm = this

            axios.post(vm.execution.links.stop)
        },
        refresh: function() {
            var vm = this

            var logContainer = this.$refs.logContainer
            vm.clearLog(logContainer)

            axios.get(vm.value.api.links.execution.default).then(response => {
                vm.execution = response.data
                vm.loadingMessages = true

                if (vm.execution.running) {
                    vm.follow()
                    return
                }

                var steps = vm.execution.links.steps.length

                var get = async function(i) {
                    await axios.get(vm.execution.links.steps[i]).then(response => {
                        response.data.forEach(function(message) {
                            vm.appendLog(logContainer, message)
                        })

                        if (i == steps - 1) {
                            vm.loaded = true
                            vm.loadingMessages = false
                        }
                    })
                }

                for (var i=0;i<steps;i++) {
                    get(i)
                }
            })
        },
        follow: function() {
            var vm = this

            var loc = window.location, new_uri
            if (loc.protocol === "https:") {
                new_uri = "wss:"
            } else {
                new_uri = "ws:"
            }
            new_uri += "//" + loc.host;
            new_uri += loc.pathname + "execution/log/stream";

            var logContainer = this.$refs.logContainer
            vm.clearLog(logContainer)

            const socket = new WebSocket(new_uri)
            socket.addEventListener('message', function (event) {
                vm.appendLog(logContainer, event.data)
            })
            socket.addEventListener('open', function (event) {
                vm.loadingMessages = true
                vm.execution.running = true
            })
            socket.addEventListener('close', function (event) {
                vm.loadingMessages = false
                vm.execution.running = false
            })
        },
        getRelativeTimestamp: function() {
            return TimeAgo.inWords(this.execution.lastExecution)
        },
        goToTop: function() {
            window.scrollTo(0, 0)
        },
        goToEnd: function() {
            var height = window.document.body.scrollHeight
            window.scrollTo(0, height)
        }
    },
    mounted: function() {
        var vm = this

        vm.refresh()
    },
    updated: function() {

    }
})
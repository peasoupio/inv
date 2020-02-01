Vue.component('install', {
    template: `
<div>
    <div style="position: sticky; top: 6em; z-index: 10">
        <div class="buttons is-right">
            <a class="is-link" @click="goToTop()" style="margin-right: 1em;">
                Go to top
            </a>
            <a class="is-link" @click="goToEnd()" style="margin-right: 1em;">
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
        <p class="subtitle is-6" v-if="!execution.running">Last execution: {{getRelativeTimestamp()}}, duration: {{getDuration()}}</p>
        <p class="subtitle is-6" v-else :key="runningTimestamp">Started: {{getStartedAgo()}}
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
            runningTimestamp: 0,
            refreshInterval: {},
            refreshTime: 1000,
            loaded: false,
            loadingMessages: true,
            execution: {},
            buffer: []
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

            axios.post(vm.execution.links.start).then(response => {
                vm.execution.lastExecutionStartedOn = Date.now()
            })
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

                if (!steps) {
                    vm.loadingMessages = false
                    return
                }

                var get = async function(i) {
                    await axios.get(vm.execution.links.steps[i]).then(response => {
                        response.data.forEach(function(message) {
                            //vm.appendLog(logContainer, message)
                            vm.buffer.push(message)
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
                //vm.appendLog(logContainer, event.data)
                vm.buffer.push(event.data)
            })
            socket.addEventListener('open', function (event) {
                vm.loadingMessages = true
                vm.execution.running = true
            })
            socket.addEventListener('close', function (event) {
                vm.loadingMessages = false

                axios.get(vm.value.api.links.execution.default).then(response => {
                    vm.execution = response.data
                    vm.execution.running = false
                })
            })
        },
        getRelativeTimestamp: function() {
            return TimeAgo.inWords(this.execution.lastExecution)
        },
        getDuration: function() {
            if (!this.execution.lastExecution)
                return

            if (!this.execution.lastExecutionStartedOn)
                return

            return new Date(this.execution.lastExecution - this.execution.lastExecutionStartedOn)
                .toISOString()
                .slice(11, -1)
        },
        getStartedAgo: function() {
            if (!this.execution.running)
                return

            return TimeAgo.inWords(this.execution.lastExecutionStartedOn)
        },
        goToTop: function() {
            window.scrollTo(0, 0)
        },
        goToEnd: function() {
            window.scrollTo(0, window.document.body.scrollHeight)
        }
    },
    mounted: function() {
        var vm = this

        var logContainer = this.$refs.logContainer
        setInterval(function() {
            if (vm.execution.running)
                vm.runningTimestamp++

            if (vm.buffer.length == 0)
                return

            while(vm.buffer.length > 0) {
                var message = vm.buffer.shift()

                vm.appendLog(logContainer, message)
            }
        }, 125)

        vm.refresh()
    },
    updated: function() {

    }
})
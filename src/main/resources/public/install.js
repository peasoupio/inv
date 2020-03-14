Vue.component('install', {
    template: `
<div>
    <div style="position: sticky; top: 6em; z-index: 10">

        <div class="buttons is-right">

            <a class="is-link breath-heavy" @click="goToTop()">
                Go to top
            </a>
            <a class="is-link breath-heavy" @click="goToEnd()">
                Go to end
            </a>

            <div class="dropdown is-hoverable">
                <div class="dropdown-trigger">
                    <button class="button is-link" aria-haspopup="true" aria-controls="dropdown-menu" :disabled="execution.running">
                        <span>Options</span>
                        <span class="icon is-small">
                            <i class="fas fa-angle-down" aria-hidden="true"></i>
                        </span>
                    </button>
                </div>
                <div class="dropdown-menu" id="dropdown-menu" role="menu" v-if="!execution.running">
                    <div class="dropdown-content">
                        <a class="dropdown-item is-link" @click="toggleDebugMode()">
                            <span>Debug</span>
                            <span class="icon is-small" v-show="enableDebugMode"><i class="fas fa-check-square"></i></span>
                            <span class="icon is-small" v-if="!enableDebugMode"><i class="far fa-square"></i></span>
                        </a>
                    </div>
                    <div class="dropdown-content">
                        <a class="dropdown-item is-link" @click="toggleSystemMode()" :disabled="execution.running">
                            <span>System</span>
                            <span class="icon is-small" v-show="enableSystemMode"><i class="fas fa-check-square"></i></span>
                            <span class="icon is-small" v-if="!enableSystemMode"><i class="far fa-square"></i></span>
                        </a>
                    </div>
                    <div class="dropdown-content">
                        <a class="dropdown-item is-link" @click="toggleSecureMode()" :disabled="execution.running">
                            <span>Secure</span>
                            <span class="icon is-small" v-show="enableSecureMode"><i class="fas fa-check-square"></i></span>
                            <span class="icon is-small" v-if="!enableSecureMode"><i class="far fa-square"></i></span>
                        </a>
                    </div>
                </div>
            </div>



            <button class="button is-info" :disabled="execution.running" @click="start()" v-bind:class=" { 'is-loading': execution.running }">
                Execute
            </button>

            <button class="button is-danger" :disabled="!execution.running" @click="stop()">
                Stop
            </button>
        </div>
    </div>

    <p class="title is-5">
        Output
        <span v-if="execution.lastExecution > 0">
            <a @click="downloadLatestLog()">(raw log)</a>
        </span>
        <span class="icon is-small" v-if="loadingMessages">
            <i class="fas fa-spinner fa-pulse"></i>
        </span>
        <p class="subtitle is-6" v-if="!execution.running">Last execution ended: {{getEndedAgo()}}, duration: {{getDuration()}}</p>
        <p class="subtitle is-6" v-else :key="runningTimestamp">Started: {{getStartedAgo()}}
    </p>

    <hr />

    <div class="output" style="height: 75vh; overflow-y: scroll; scroll-behavior: smooth;">
        <div ref="logContainer" ></div>
        <div class="anchor"></div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {

            enableSecureMode: true,
            enableDebugMode: false,
            enableSystemMode: false,

            runningTimestamp: 0,

            loaded: false,
            loadingMessages: true,
            execution: {},
            bufferProcessMaxSize: 5120,
            bufferProcessSize: 1024,
            bufferProcessCycleMs: 2000,
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
            var vm = this

            if (logContainer.children.length > vm.bufferProcessMaxSize) {
                for(var i=0;i<12;i++) {
                    logContainer.removeChild(logContainer.childNodes[0])
                }
            }

            var pre = document.createElement("PRE")
            pre.appendChild(document.createTextNode(message))

            logContainer.appendChild(pre)
        },
        downloadLatestLog: function() {
            var vm = this
            var latestLog = vm.execution.lastExecution

            axios({
                url: vm.value.api.links.execution.downloadLatestLog,
                method: 'GET',
                responseType: 'blob',
            }).then((response) => {
                var fileURL = window.URL.createObjectURL(new Blob([response.data]));
                var fileLink = document.createElement('a');

                fileLink.href = fileURL;
                fileLink.setAttribute('download', 'inv-' + latestLog + '-log.txt');
                document.body.appendChild(fileLink);

                fileLink.click();
            })
        },
        toggleDebugMode: function() {
            var vm = this

            vm.enableDebugMode = !vm.enableDebugMode
            localStorage.enableDebugMode = vm.enableDebugMode

            vm.enableSystemMode = false
            localStorage.enableSystemMode = false
        },
        toggleSystemMode: function() {
            var vm = this

            vm.enableSystemMode = !vm.enableSystemMode
            localStorage.enableSystemMode = vm.enableSystemMode

            vm.enableDebugMode = false
            localStorage.enableDebugMode = false
        },
        toggleSecureMode: function() {
            var vm = this

            vm.enableSecureMode = !vm.enableSecureMode
            localStorage.enableSecureMode = vm.enableSecureMode
        },
        start: function() {
            var vm = this

            if (vm.execution.running)
                return

            var cfg = {
                debugMode: vm.enableDebugMode,
                systemMode: vm.enableSystemMode,
                secureMode: vm.enableSecureMode
            }

            axios.post(vm.execution.links.start, cfg).then(response => {
                vm.execution.lastExecutionStartedOn = Date.now()
                location.reload()
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

            axios.get(vm.value.api.links.execution.default).then(async (response) => {
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

                var fetchLog = async function(index) {
                    return axios.get(vm.execution.links.steps[index]).then(response => {
                        response.data.forEach(function(message) {
                            vm.buffer.push(message)
                        })

                        if (i == steps - 1) {
                            vm.loaded = true
                            vm.loadingMessages = false
                        }
                    })
                }

                for (var i=0;i<steps;i++) {
                    await fetchLog(i)
                }
            })
        },
        follow: function() {
            var vm = this

            var logContainer = this.$refs.logContainer
            vm.clearLog(logContainer)

            const socket = new WebSocket(websocketHost() + vm.value.api.links.execution.stream)
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
        getEndedAgo: function() {
            return TimeAgo.inWords(this.execution.lastExecution)
        },
        getDuration: function() {
            if (!this.execution.lastExecution)
                return "never happened"

            if (!this.execution.lastExecutionStartedOn)
                return "never happened"

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
            var logContainerParent = this.$refs.logContainer.parentElement
            logContainerParent.scrollTo(0, 0)
        },
        goToEnd: function() {
            var logContainerParent = this.$refs.logContainer.parentElement
            logContainerParent.scrollTo(0, logContainerParent.scrollHeight)
        }
    },
    mounted: function() {
        var vm = this

        if (localStorage.enableDebugMode != undefined)
            vm.enableDebugMode = localStorage.enableDebugMode == "true" ? true : false

        if (localStorage.enableSystemMode != undefined)
            vm.enableSystemMode = localStorage.enableSystemMode == "true" ? true : false

        if (localStorage.enableSecureMode != undefined)
            vm.enableSecureMode = localStorage.enableSecureMode == "true" ? true : false

        var logContainer = this.$refs.logContainer
        setInterval(function() {
            if (vm.execution.running)
                vm.runningTimestamp++

            if (vm.buffer.length == 0)
                return

            var maxPerCycle = vm.bufferProcessSize
            while(maxPerCycle > 0 && vm.buffer.length > 0) {
                maxPerCycle--

                var message = vm.buffer.shift()
                vm.appendLog(logContainer, message)
            }
        }, vm.bufferProcessCycleMs)

        vm.refresh()
    }
})
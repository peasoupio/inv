Vue.component('install', {
    template: `
<div>
    <div class="buttons is-right">
        <button class="button breathe-heavy" @click="toggleFeature('autoRefresh')" v-bind:class="{ 'is-link': features.autoRefresh }">
            <span>Auto-refresh (5 secs)</span>
        </button>

        <div class="dropdown is-hoverable">
            <div class="dropdown-trigger">
                <button class="button" aria-haspopup="true" aria-controls="dropdown-menu" :disabled="execution.running">
                    <span>Options</span>
                    <span class="icon is-small">
                        <i class="fas fa-angle-down" aria-hidden="true"></i>
                    </span>
                </button>
            </div>
            <div class="dropdown-menu" id="dropdown-menu" role="menu" v-if="!execution.running">
                <div class="dropdown-content">
                    <a class="dropdown-item is-link" @click="toggleFeature('debugMode') + turnOffFeature('systemMode')">
                        <span>Debug</span>
                        <span class="icon is-small" v-show="features.debugMode"><i class="fas fa-check-square"></i></span>
                        <span class="icon is-small" v-if="!features.debugMode"><i class="far fa-square"></i></span>
                    </a>
                </div>
                <div class="dropdown-content">
                    <a class="dropdown-item is-link" @click="toggleFeature('systemMode') + turnOffFeature('debugMode')" :disabled="execution.running">
                        <span>System</span>
                        <span class="icon is-small" v-show="features.systemMode"><i class="fas fa-check-square"></i></span>
                        <span class="icon is-small" v-if="!features.systemMode"><i class="far fa-square"></i></span>
                    </a>
                </div>
                <div class="dropdown-content">
                    <a class="dropdown-item is-link" @click="toggleFeature('secureMode')" :disabled="execution.running">
                        <span>Secure</span>
                        <span class="icon is-small" v-show="features.secureMode"><i class="fas fa-check-square"></i></span>
                        <span class="icon is-small" v-if="!features.secureMode"><i class="far fa-square"></i></span>
                    </a>
                </div>
            </div>
        </div>
    </div>

    <p class="title is-4 has-text-centered" v-if="!execution.running">
        Ready to execute ?
    </p>

    <p class="title is-4 has-text-centered" v-if="execution.running">
        <span class="icon is-small">
            <i class="fas fa-spinner fa-pulse"></i>
        </span> Running
        <span v-if="execution.links && execution.links.stream">(<a href="/logtrotter.html" target="_blank">Follow logs</a>)</span>
    </p>

    <div class="buttons is-centered">
        <button class="button is-info" @click="start()" v-if="!execution.running">
            Execute
        </button>

        <button class="button is-danger is-outlined" @click="stop()" v-if="execution.running">
            Stop
        </button>
    </div>

    <hr />

    <p class="title is-5">Latest execution report</p>

    <div class="content">
        <p class="title is-6">
            <span v-if="!execution.running">Lasted: {{getDuration()}}</span>
            <span v-if="execution.running">Started: {{getStartedAgo()}}</span>
            <span v-if="execution.links && execution.links.download">
                (<a @click="downloadLatestLog()">download logs</a><span v-if="getFileSize()">, {{getFileSize()}}</span>)
            </span>
        </p>
    </div>

    <review v-model="value" :update="updateIndex" />
</div>
`,
    props: ['value'],
    data: function() {
        return {

            features: {
                autoRefresh: false,
                secureMode: false,
                debugMode: false,
                systemMode: false
            },

            execution: {},
            runningTimestamp: 0,
            socket: undefined,
            filters: {
                scm: ''
            },
            updateIndex: 0
        }
    },
    methods: {
        downloadLatestLog: function() {
            var vm = this
            var latestLog = vm.execution.lastExecution.startedOn

            axios({
                url: vm.execution.links.download,
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
        toggleFeature: function(name) {
            var vm = this

            vm.features[name] = !vm.features[name]
            localStorage[name] = vm.features[name]
        },
        turnOffFeature: function(name) {
            var vm = this

            vm.features[name] = false
            localStorage[name] = false
        },
        turnOnFeature: function(name) {
            var vm = this

            vm.features[name] = true
            localStorage[name] = true
        },
        start: function() {
            var vm = this

            if (vm.execution.running)
                return

            var cfg = {
                debugMode: vm.features.debugMode,
                systemMode: vm.features.systemMode,
                secureMode: vm.features.secureMode
            }

            axios.post(vm.execution.links.start, cfg).then(response => {
                location.reload()
            })
        },
        stop: function() {
            var vm = this

            axios.post(vm.execution.links.stop).then(response => {
               location.reload()
            })
        },
        refresh: function() {
            var vm = this

            axios.get(vm.value.api.links.execution.default).then(async (response) => {
                vm.execution = response.data
            })
        },
        autoRefresh: function() {
            var vm = this

            setInterval(function() {
                if (!vm.features.autoRefresh)
                    return

                if (!vm.execution.running)
                    return

                vm.refresh()
                vm.updateIndex++

            }, 5000)
        },
        getEndedAgo: function() {
            var vm = this

            if (!vm.execution.lastExecution)
                return "never happened"

            return TimeAgo.inWords(vm.execution.lastExecution.endedOn)
        },
        getDuration: function() {
            var vm = this

            if (!vm.execution.lastExecution)
                return "never happened"

            if (!vm.execution.lastExecution.endedOn)
                return "never happened"

            if (!vm.execution.lastExecution.startedOn)
                return "never happened"

            return new Date(vm.execution.lastExecution.endedOn - vm.execution.lastExecution.startedOn)
                .toISOString()
                .slice(11, -1)
        },
        getStartedAgo: function() {
            var vm = this

            if (!vm.execution.running)
                return

            return TimeAgo.inWords(vm.execution.lastExecution.startedOn)
        },
        getFileSize: function() {
            var logSize = this.execution.lastExecution.logSize
            if (!logSize)
                return undefined

            var thresh = 1024;
            if(Math.abs(logSize) < thresh) {
                return logSize + ' B'
            }
            var units = ['KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB']
            var u = -1;
            do {
                logSize /= thresh;
                ++u;
            } while(Math.abs(logSize) >= thresh && u < units.length - 1);
            return logSize.toFixed(1)+' '+units[u];
        },
        getLastExecutedScm: function() {
            var vm = this

            if (!vm.execution || !vm.execution.lastExecution)
                return []

            var filtered = vm.execution.lastExecution.scms.filter(function(scm) {
                if (scm.indexOf(vm.filters.scm) < 0) return

                return true
            })
            filtered.sort()

            return filtered
        }
    },
    mounted: function() {
        var vm = this

        if (localStorage.autoRefresh == "true")
            vm.turnOnFeature('autoRefresh')

        if (localStorage.debugMode == "true")
            vm.turnOnFeature('debugMode')

        if (localStorage.systemMode == "true")
            vm.turnOnFeature('systemMode')

        if (localStorage.secureMode == undefined || localStorage.secureMode == "true")
            vm.turnOnFeature('secureMode')

        vm.refresh()
        vm.autoRefresh()
    }
})
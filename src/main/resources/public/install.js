Vue.component('install', {
    template: `
<div>
    <p class="title is-1">Install: </p>

    <div>
        <span>Ready to run ? </span>
        <button class="button is-info" :disabled="execution.running" @click="start()" v-bind:class=" { 'is-loading': execution.running }">Ready</button>
    </div>

    <p class="title is-5">
        Output
        <span class="icon is-small" v-if="loadingMessages">
            <i class="fas fa-spinner fa-pulse"></i>
        </span>
    </p>
    <div style="overflow-y: scroll; height: 600px; width: 100%" ref="logContainer">
        <pre style="padding: 0; white-space: pre-wrap" v-for="(message, index) in messages">{{message}}</pre>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            loaded: false,
            loadingMessages: true,
            execution: {},
            lastIndex: 0,
            messages: []
        }
    },
    methods: {
        start: function() {
            var vm = this

            if (vm.execution.running)
                return

            vm.messages = []
            vm.lastIndex = 0
            vm.loadingMessages = true
            vm.execution.running = true

            axios.post(vm.execution.links.start)
        },

        stop: function() {
            var vm = this

            axios.post(execution.links.stop)
        },

        refresh: function() {
            var vm = this

            if (vm.loaded) {

                var running = vm.execution.running
                var missingSteps = vm.lastIndex != vm.execution.links.steps.length

                if (!running && !missingSteps) {
                    vm.loadingMessages = false
                    return
                }
            }

            axios.get(vm.value.api.links.execution.default).then(response => {
                vm.execution = response.data

                vm.loaded = true

                if (vm.lastIndex == vm.execution.links.steps.length)
                    return

                axios.get(vm.execution.links.steps[vm.lastIndex]).then(response => {

                    vm.lastIndex++

                    response.data.forEach(function(message) {
                        vm.messages.push(message)
                    })
                })
            })
        }
    },
    created: function() {

        var vm = this

        vm.refresh()

        setInterval(function() {
            vm.refresh()
        }, 1000)
    },
    updated: function() {
        var element = this.$refs.logContainer
        element.scrollTop = element.scrollHeight
    }
})
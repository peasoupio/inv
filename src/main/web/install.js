Vue.component('install', {
    template: `
<div>
    <p class="title is-1">Install: </p>

    <div>
        <span>Ready to run ? </span>
        <button class="button is-info" :disabled="execution.running" @click="start()" v-bind:class=" { 'is-loading': execution.running }">Ready</button>
    </div>

    <div>
        <p class="title is-5">Output: </p>
        <p v-for="(message, index) in messages">
            <pre style="padding: 0">{{message}}</pre>
        </p>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
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
            vm.execution.running = true

            axios.post(vm.execution.links.start)
        },

        stop: function() {
            var vm = this

            axios.post(execution.links.stop)
        },

        refresh: function() {
            var vm = this

            if (vm.execution.running != null && !vm.execution.running)
                return

            axios.get("/execution").then(response => {
                vm.execution = response.data

                if (vm.lastIndex == vm.execution.links.steps.length)
                    return

                for(var i = vm.lastIndex; i < vm.execution.links.steps.length; i++) {
                    axios.get(vm.execution.links.steps[i]).then(response => {

                        response.data.forEach(function(message) {
                            vm.messages.push(message)
                        })

                    })
                }

                vm.lastIndex = vm.execution.links.steps.length
            })
        }
    },
    created: function() {

        var vm = this

        vm.refresh()

        setInterval(function() {
            vm.refresh()
        }, 1500)
    }
})
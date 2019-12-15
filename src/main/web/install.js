Vue.component('install', {
    template: `
<div>
    <p class="title is-1">Install: </p>

    <div>
        <span>Ready to run ? </span>
        <button class="button is-info" :disabled="execution.running" v-on:click="start()" v-bind:class=" { 'is-loading': execution.running }">Ready</button>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            execution: {}
        }
    },
    methods: {
        start: function() {
            var vm = this

            if (vm.execution.running)
                return

            vm.execution.running = true

            axios.post(vm.execution.links.start).then(response => {

            })
        },

        stop: function() {
            var vm = this

            axios.post(execution.links.stop).then(response => {

            })
        }
    },
    created: function() {

        var vm = this

        axios.get("/execution").then(response => {
            vm.execution = response.data
        })
    }
})
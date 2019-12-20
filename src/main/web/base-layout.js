Vue.component('base-layout', {
    template: `
<div class="columns">
    <div class="column is-2 sidebar">
        <p class="title is-1">Inv</p>
        <p class="subtitle is-3">Composer</p>
        <hr />
        <div>
            <ul class="steps is-vertical">

                <li v-for="(step, index) in steps"
                    class="steps-segment"
                    v-bind:class=" { 'is-active': currentStep == step.template } ">

                    <span href="#" class="steps-marker">{{index + 1}}</span>
                    <div class="steps-content">
                        <p class="is-size-4"><a @click="currentStep = step.template">{{step.name}}</a></p>
                        <p>{{step.description}}</p>
                    </div>
                </li>

            </ul>
        </div>
    </div>
    <div class="column is-10" v-if="ready()">
        <component v-bind:is="currentStep" v-model="shared" />
    </div>
</div>
`,
    data: function() {
        return {
            currentStep: 'choose',
            steps: [
                { name: 'Choose', template: 'choose', description: 'Choose your INVs' },
                { name: 'Configure', template: 'configure', description: 'Configure your parameters and scms' },
                { name: 'Review', template: 'review', description: 'Review everything' },
                { name: 'Install', template: 'install', description: 'Generate and install your freshly new INV ecosystem' }
            ],
            shared: {
                scms: {},
                invs: [],
                requiredInvs: []
            },
            loadState: {
                scm: false,
                invs: false
            }
        }
    },
    methods: {
        ready: function() {
            return this.loadState.scms && this.loadState.invs
        }
    },
    created: function() {
        var vm = this


        axios.get('/run').then(response => {

            vm.shared.invs = response.data

            vm.loadState.invs = true
            vm.$forceUpdate()
        })

        axios.get('/scms').then(response => {
            vm.shared.scms = response.data

            vm.loadState.scms = true
            vm.$forceUpdate()
        })
    }
})
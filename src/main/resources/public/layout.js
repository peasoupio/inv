Vue.component('layout', {
    template: `
<div class="mainContent"  style="padding: 1em">
    <div>
        <p class="title is-1">INV</p>
        <p class="subtitle is-3">Composer</p>

        <div class="steps">
            <div class="step-item" v-for="(step, index) in steps" v-bind:class="{ 'is-active': isSelected(step), 'is-completed': isCompleted(step) }">
                <div class="step-marker">{{index + 1}}</div>
                <div class="step-details">
                    <p class="step-title">{{step.name}}</p>
                    <p>{{step.description}}</p>
                </div>
            </div>
            <div class="steps-actions" style="padding-top: 3em;">
                <div class="steps-action">
                    <a @click="previousStep()" data-nav="previous" class="button" :disabled="currentStep.index - 1 == 0">Previous step</a>
                </div>
                <div class="steps-action">
                    <a @click="nextStep()" data-nav="next" class="button" :disabled="currentStep.index == steps.length">Next step</a>
                </div>
            </div>
        </div>
    </div>

    <hr />

    <div v-if="ready()" style="min-height: 900px">
        <p class="title is-2">{{currentStep.name}}: </p>
        <component v-bind:is="currentStep.template" v-model="shared" />
    </div>
</div>
`,
    data: function() {
        return {
            currentStep: {},
            steps: [
                { name: 'Choose', template: 'choose', description: 'Choose your INVs', index: 1 },
                { name: 'Configure', template: 'configure', description: 'Configure your parameters and scms', index: 2 },
                { name: 'Install', template: 'install', description: 'Generate and install your freshly new INV ecosystem', index: 3 },
                { name: 'Review', template: 'review', description: 'Review everything', index: 4 }
            ],
            shared: {
                api: {},
                scms: {},
                selectedScms: [],
                invs: {},
                requiredInvs: {}
            }
        }
    },
    methods: {
        ready: function() {
            return this.currentStep && this.shared.api.links != undefined
        },
        isSelected: function(step) {
            return this.currentStep == step
        },
        isCompleted: function(step) {
            return step.index < this.currentStep.index
        },
        nextStep: function() {
            var vm  = this

            if (vm.currentStep.index == vm.steps.length)
                return

            var next = vm.steps.filter(function(other) {
                return other.index == vm.currentStep.index + 1
            })

            vm.currentStep = next[0]
        },
        previousStep: function() {
            var vm  = this

            if (vm.currentStep.index - 1 == 0)
                return

            var previous = vm.steps.filter(function(other) {
                return other.index == vm.currentStep.index - 1
            })

            vm.currentStep = previous[0]
        },
    },
    created: function() {
        var vm = this

        // Init steps
        vm.currentStep = vm.steps[0]

        axios.get('/api').then(response => {

            vm.shared.api = response.data

            axios.get(vm.shared.api.links.scms.default).then(response => {
                vm.shared.scms = response.data

                vm.$forceUpdate()
            })
        })
    }
})

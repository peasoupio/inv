Vue.component('layout', {
    template: `
<div class="mainContent"  style="padding: 1em">

    <p class="title is-1">INV <span class="subtitle is-3">Composer</span></p>

    <hr />

    <div class="columns header">
        <div class="column is-2" style="display: inline-grid; align-items: center;">
            <a @click="previousStep()" data-nav="previous" class="button" :disabled="currentStep.index - 1 == 0">Previous step</a>
        </div>
        <div class="column">
            <div class="steps">
                <div class="step-item" v-for="(step, index) in steps" v-bind:class="{ 'is-active': isSelected(step), 'is-completed': isCompleted(step) }">
                    <div class="step-marker">{{index + 1}}</div>
                    <div class="step-details" @mouseleave="step.showHelp = false">
                        <span class="step-title">
                            {{step.name}}
                        </span>
                        <a class="icon" @mouseover="step.showHelp = true" style="margin-right: 0.75em">
                            <i class="fas fa-question-circle" aria-hidden="true"></i>
                        </a>
                        <div class="notification is-primary" v-show="step.showHelp" style="position: absolute; width: 100%">
                            <p v-html="step.description"></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="column is-2" style="display: inline-grid; align-items: center;">
            <a @click="nextStep()" data-nav="next" class="button" :disabled="currentStep.index == steps.length">Next step</a>
        </div>
    </div>

    <hr />

    <div v-if="ready()" style="min-height: 75vh;">
        <p class="title is-2">{{currentStep.name}}: </p>
        <component v-bind:is="currentStep.template" v-model="shared" />
    </div>
</div>
`,
    data: function() {
        return {
            currentStep: {},
            steps: [
                { name: 'Choose', template: 'choose', index: 1, showHelp: false, description: 'Choose your INVs'  },
                { name: 'Configure', template: 'configure', index: 2, showHelp: false, description: 'Configure your parameters and scms' },
                { name: 'Install', template: 'install', index: 3, showHelp: false, description: 'Generate and install your freshly new INV ecosystem' },
                { name: 'Review', template: 'review', index: 4, showHelp: false, description: 'Review everything' }
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
        setCurrentStep: function(step) {
            var vm = this

            document.title = "INV - Composer - " + step.name
            window.location.hash = "#" + step.template

            vm.currentStep = step
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

            vm.setCurrentStep(next[0])
        },
        previousStep: function() {
            var vm  = this

            if (vm.currentStep.index - 1 == 0)
                return

            var previous = vm.steps.filter(function(other) {
                return other.index == vm.currentStep.index - 1
            })

            vm.setCurrentStep(previous[0])
        },
    },
    mounted: function() {
        var vm = this

        // Init steps
        var hash = window.location.hash
        vm.setCurrentStep(vm.steps[0])

        vm.steps.forEach(function(step) {
            if (hash !== '#' + step.template)
                return

            vm.setCurrentStep(step)
        })

        axios.get('/api').then(response => {

            vm.shared.api = response.data

            axios.get(vm.shared.api.links.scms.default).then(response => {
                vm.shared.scms = response.data

                vm.$forceUpdate()
            })
        })
    }
})
Vue.component('first-time', {
    template: `
<div class="modal is-active" v-if="!closed">
    <div class="modal-background"></div>
    <div class="modal-content" style="width: 50%">
        <div class="box" v-click-outside="close">
            <div class="content">
                <p class="has-text-warning has-text-centered title is-4">Oopsy, it seems no run information is available.</p>
                <p>Have you considered the following?</p>
                <ul>
                    <li>The error basically means no "run.txt" is available within Composer's reach</li>
                    <li>The first-time usage does not provide a default "run.txt" file. You must start the process by choosing SCM. Take a look at "By SCM". Upon successful completion, "run.txt" will be available.</li>
                    <li>Check on your filesystem under INV_HOME path or the current Composer execution path if "run.txt" is present</li>
                </ul>
            </div>
            <p class="has-text-right">
                <a @click="toggleFirstTimeNotShowAgain()">
                    Show this message again?
                    <span v-show="firstTimeNotShowAgain()"><i class="fas fa-check-square"></i></span>
                    <span v-show="!firstTimeNotShowAgain()"><i class="far fa-square"></i></span>
                </a>
            </p>
        </div>
    </div>
</div>
`,
    data: function() {
        return {
           closed: false
        }
    },
    methods: {
        firstTimeNotShowAgain: function() {
            if (localStorage.firstTimeNotShowAgain == undefined)
                return false

            return localStorage.firstTimeNotShowAgain === "false"
        },
        toggleFirstTimeNotShowAgain: function() {
            if (this.firstTimeNotShowAgain())
                localStorage.firstTimeNotShowAgain = "true"
            else
                localStorage.firstTimeNotShowAgain = "false"

            this.$forceUpdate()

        },
        close: function() {
            this.closed = true
        }
    },
    mounted: function() {
        this.closed = this.firstTimeNotShowAgain()
    }
})

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
        <p class="title is-2">{{currentStep.name}}:</p>
        <p class="subtitle is-5">{{currentStep.description}}</p>
        <component v-bind:is="currentStep.template" v-model="shared" />
    </div>

    <first-time v-if="shared.setup.firstTime"></first-time>
</div>
`,
    data: function() {
        return {
            currentStep: {},
            steps: [
                { name: 'Choose', template: 'choose', index: 1, showHelp: false, description: 'Choose your INVs'  },
                { name: 'Configure', template: 'configure', index: 2, showHelp: false, description: 'Configure your parameters and scms' },
                { name: 'Install', template: 'install', index: 3, showHelp: false, description: 'Generate and install your freshly new INV ecosystem' },
                { name: 'Review', template: 'review', index: 4, showHelp: false, description: 'Review added or missing broadcasts' },
                { name: 'Promote', template: 'promote', index: 5, showHelp: false, description: 'Promote latest run' }
            ],
            shared: {
                api: {},
                setup: {},
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


            axios.get(vm.shared.api.links.setup).then(response => {
                vm.shared.setup = response.data
            })

            /*
            axios.get(vm.shared.api.links.scms.default).then(response => {
                vm.shared.scms = response.data

                vm.$forceUpdate()
            })
            */
        })
    }
})
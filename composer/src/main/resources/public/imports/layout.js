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
                    <li>The first-time usage does not provide a default "run.txt" file. You must start the process by choosing REPO. Take a look at "By REPO
                    ". Upon successful completion, "run.txt" will be available.</li>
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
            if (localStorage.firstTimeNotShowAgain === undefined)
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
<div>
    <nav class="navbar" role="navigation" aria-label="main navigation">
      <div class="navbar-brand">
        <a class="navbar-item" @click="resetStep()">
          <img src="logo.png" alt="Peasoup INV: Open source, environment as code solution" width="112" height="28">
        </a>

        <a role="button" class="navbar-burger burger" aria-label="menu" aria-expanded="false"
            v-bind:class="{ 'is-active': showBurger }"
            @click="showBurger=!showBurger">
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
        </a>
      </div>

      <div class="navbar-menu" v-bind:class="{ 'is-active': showBurger }">
        <div class="navbar-start">
          <div class="navbar-item has-dropdown is-hoverable">
              <a class="navbar-link">
                Settings
              </a>

              <div class="navbar-dropdown">
                <a class="navbar-item" @click.stop="showConfigureRun()">Edit run.txt file</a>
                <hr class="navbar-divider">
                <a class="navbar-item" @click.stop="showGlobalSettings()">Edit global settings</a>
                <a class="navbar-item" @click.stop="showConfigureREPOs()">Edit REPOs</a>
                <a class="navbar-item" @click.stop="showConfigureInit()">Edit init file</a>
                <hr class="navbar-divider">
                <a class="navbar-item" @click.stop="pullInit()">Pull changes</a>
                <a class="navbar-item" @click.stop="pushInit()">Push changes</a>
              </div>
            </div>

          <a class="navbar-item" href="https://github.com/peasoupio/inv/wiki" target="_blank">
            Documentation
          </a>

          <div class="navbar-item has-dropdown is-hoverable">
            <a class="navbar-link">
              More
            </a>

            <div class="navbar-dropdown">
              <a class="navbar-item" href="https://github.com/peasoupio/inv" target="_blank"><i class="fab fa-github"></i><span style="padding-left: 0.25em">@peasoupio/inv</span></a>
              <hr class="navbar-divider">
              <a class="navbar-item" href="https://github.com/peasoupio/inv/issues" target="_blank">Report an issue</a>
            </div>
          </div>

          <div class="navbar-item" @mouseleave="showHelpSecured = false">
              <div class="buttons" v-if="shared.setup.secured" @mouseover="showHelpSecured = true">
                <button class="button">
                  Admin mode
                </button>
                <div class="notification is-primary" v-show="showHelpSecured" style="position: absolute; inline-size: fit-content;; top: 100%;">
                    <p>The current session is secured.</p>
                    <p>You are allowed to do administrative tasks.</p>
                </div>
              </div>
            </div>
        </div>

        <div class="navbar-end" v-if="shared.setup">

          <div class="navbar-item" v-if="shared.setup.initInfo">
            <div class="buttons" v-if="!shared.setup.initInfo.standalone">
                <a class="button" target="_blank" :href="shared.setup.initInfo.source">
                    <span class="has-text-link has-text-weight-semibold">{{shared.setup.initInfo.version}}</span><span class="has-text-weight-semibold">@{{shared.setup.initInfo.source}}</span>
                </a>
            </div>
            <div class="buttons" v-if="shared.setup.initInfo.standalone">
                <button class="button" disabled>
                    Source: Standalone
                </button>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <div class="mainContent">

        <div class="pageloader" v-bind:class="{ 'is-active': shared.setup.booted == false }">
            <span class="title">Booting... {{progression()}}</span>
        </div>

        <div v-for="element in navbarElements()">
            <component v-bind:is="element.template" v-model="element.model"  v-if="element.model.visible" />
        </div>

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

        <div v-if="ready()" style="min-height: 75vh;">
            <p class="title is-2">{{currentStep.name}}:</p>
            <p class="subtitle is-5">{{currentStep.description}}</p>
            <component v-bind:is="currentStep.template" v-model="shared" />
        </div>

        <first-time v-if="shared.setup.firstTime"></first-time>
    </div>

    <toast />

    <footer class="footer">
        <div class="content has-text-centered">
            <p>
                <strong>INV - Composer</strong> by <a href="https://peasoup.io">peasoup.io</a>. The source code is licensed <a href="https://github.com/peasoupio/inv/blob/master/LICENSE">GPL-3.0</a>.<br />
                The website content is licensed <a href="http://https://creativecommons.org/licenses/by-nd/4.0/">CC BY-ND 4.0</a>.<br />
                Release version: <strong>{{shared.setup.releaseVersion}}</strong>
            </p>
        </div>
    </footer>
</div>
`,
    data: function() {
        return {
            showBurger: false,
            showHelpSecured: false,
            currentStep: {},
            bootData: {
                todo: 0,
                done: 0
            },
            navbar: {
                configureRun: {
                    model   : { visible: false },
                    template: 'configure-run'
                },
                configureSettings: {
                    model   : { visible: false },
                    template: 'configure-settings'
                },
                configureREPOs: {
                    model   : { visible: false },
                    template: 'configure-repos'
                },
                configureInit: {
                    model   : { visible: false },
                    template: 'configure-init'
                }
            },
            steps: [
                { name: 'Choose', template: 'choose', index: 1, showHelp: false, description: 'Choose your INVs'  },
                { name: 'Configure', template: 'configure', index: 2, showHelp: false, description: 'Configure your parameters and REPOs' },
                { name: 'Install', template: 'install', index: 3, showHelp: false, description: 'Generate and install your freshly new INV ecosystem' },
                { name: 'Promote', template: 'promote', index: 4, showHelp: false, description: 'Promote latest run' }
            ],
            shared: {
                api: {},
                setup: {}
            }
        }
    },
    methods: {

        navbarElements: function() {
            const vm = this
            const list = []

            Object.keys(vm.navbar).forEach(function(element) {
                list.push(vm.navbar[element])
            })

            return list

        },

        setup: function() {
            const vm = this

            // Get Setup Data
            axios.get(vm.shared.api.links.setup).then(response => {
                vm.shared.setup = response.data

                if (!vm.shared.setup.booted) {
                    vm.followBoot()
                }
            })
        },

        followBoot: function() {
            const vm = this

            const socket = new WebSocket(websocketHost() + vm.shared.setup.links.stream)
            socket.addEventListener('message', function (event) {
                const data = JSON.parse(event.data)

                vm.bootData.todo = data.thingsToDo
                vm.bootData.done = data.thingsDone
            })
            socket.addEventListener('open', function (event) {

            })
            socket.addEventListener('close', function (event) {
                vm.setup()
            })
        },

        progression: function() {
            const done = this.bootData.done
            const todo = this.bootData.todo

            if (!done || !todo)
                return '0%'

            const percent = done / todo * 100
            return parseFloat(percent).toFixed(0)+"%"
        },

        ready: function() {
            const vm = this

            return vm.currentStep &&
                   vm.shared.api.links !== undefined &&
                   vm.shared.setup.booted
        },

        setCurrentStep: function(step) {
            const vm = this

            document.title = "INV - Composer - " + step.name
            window.location.hash = "#" + step.template

            vm.currentStep = step
        },

        resetStep: function() {
            const vm = this
            vm.setCurrentStep(vm.steps[0])
        },

        isSelected: function(step) {
            return this.currentStep === step
        },

        isCompleted: function(step) {
            return step.index < this.currentStep.index
        },

        nextStep: function() {
            const vm  = this

            if (vm.currentStep.index === vm.steps.length)
                return

            const next = vm.steps.filter(function(other) {
                return other.index === vm.currentStep.index + 1
            })

            vm.setCurrentStep(next[0])
        },

        previousStep: function() {
            const vm  = this

            if (vm.currentStep.index - 1 === 0)
                return

            const previous = vm.steps.filter(function(other) {
                return other.index === vm.currentStep.index - 1
            })

            vm.setCurrentStep(previous[0])
        },

        showConfigureRun: function() {
            this.navbar.configureRun.model.visible = true
        },

        showGlobalSettings: function() {
            this.navbar.configureSettings.model.visible = true
        },

        showConfigureREPOs: function() {
            this.navbar.configureREPOs.model.visible = true
        },

        showConfigureInit: function() {
            this.navbar.configureInit.model.visible = true
        },

        pullInit: function() {
            const vm = this

            axios.post(vm.shared.api.links.initFile.pull).then(() => {
                vm.$bus.$emit('toast', `success:Pulled <strong>init file changes</strong> successfully!`)
            })
            .catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>pull init file changes</strong>!`)
            })

            vm.$bus.$emit('toast', `Pulled <strong>init file changes</strong> requested!`)
        },

        pushInit: function() {
            const vm = this
            axios.post(vm.shared.api.links.initFile.push).then(() => {
                vm.$bus.$emit('toast', `success:Pushed <strong>init file changes</strong> successfully!`)
            })
            .catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>push init file changes</strong>!`)
            })

            vm.$bus.$emit('toast', `success:Pushed <strong>init file changes</strong> requested!`)
        }
    },
    mounted: function() {
        const vm = this

        // Init steps
        const hash = window.location.hash
        vm.resetStep()

        // Select step if # defined
        vm.steps.forEach(function(step) {
            if (hash !== '#' + step.template)
                return

            vm.setCurrentStep(step)
        })

        // Get API
        axios.get('/api/v1').then(response => {
            vm.shared.api = response.data

            // Setups menus
            vm.navbarElements().forEach(function(element) {
                element.model.shared = vm.shared
            })

            // Proceed to setup
            vm.setup()
        })

        // Check if resize, to reset burger
        vm.$nextTick(() => {
            window.addEventListener('resize', () => {
                vm.showBurger = false
            })
        })
    }
})

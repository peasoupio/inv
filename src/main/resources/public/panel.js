Vue.component('panel', {
    template: `
<nav class="panel">
    <p class="panel-heading">
        <a class="icon is-pulled-left" v-show="value.help" @click="showHelp = !showHelp" style="margin-right: 0.75em">
            <i class="fas fa-question-circle" aria-hidden="true"></i>
        </a>

        {{value.title}} ({{selected}}/{{value.total}})


        <a class="icon is-pulled-right" v-show="value.close" @click="value.close()">
            <i class="fas fa-times-circle" aria-hidden="true"></i>
        </a>
    </p>

    <div class="notification is-primary" v-show="showHelp">
        <div class="content panel-help" v-html="value.help"></div>
    </div>

    <div class="panel-block">
        <p class="control has-icons-left">
            <input class="input" type="text" placeholder="Search" v-model="filters.input" @keyup="raiseFilterKeyUp()">
            <span class="icon is-left">
                <i class="fas fa-search" aria-hidden="true"></i>
            </span>
        </p>
    </div>

    <p class="panel-tabs">
        <a v-for="filterPanel in value.filters"
            @click="filterPanel.name = filters.panel"
            v-bind:class="{ 'is-active': filterPanel.name == filters.panel }">
        </a>
    </p>

    <div class="container" v-if="value.elements.length == 0">
        <p class="has-text-centered">Nothing to show</p>
    </div>

    <span v-for="element in filter()">
        <a class="panel-block"
            @click="click(element)"
            v-bind:class="{ 'is-active': element.active, 'disabled': !element.clickable }">

            <span class="panel-icon" v-if="!element.clickable">
                <i class="fas fa-lock" aria-hidden="true"></i>
            </span>
            <span v-if="element.clickable">
                <span class="panel-icon" v-if="element.icon">
                    <i class="fas" aria-hidden="true" :class="element.icon"></i>
                </span>
                <span class="panel-icon" v-if="value.icon">
                    <i class="fas" aria-hidden="true" :class="value.icon"></i>
                </span>
            </span>

            <span class="tag is-primary" v-if="element.subLabel" style="margin-right: 0.75em">{{element.subLabel}}</span>

            {{element.label}}


        </a>
    </span>

    <div style="padding: 1em">
        <pagination v-model="paginationSettings()" />
    </div>


    <div class="panel-block">
        <button class="button is-link is-outlined is-fullwidth" @click="filters.input = ''">
            Reset filter
        </button>
    </div>

    <div class="panel-block" v-if="value.options != undefined">
        <button v-for="option in value.options"
                class="button is-link is-outlined is-fullwidth" @click="option.click()">
                {{option.label}}
        </button>
    </div>
</nav>
`,
    props: ['value'],
    data: function() {
        return {
            showHelp: false,
            filters: {
                input: '',
                panel: '',
                from: 0,
                step: 10
            }
        }
    },
    computed: {
        isStatic: {
            get() {
                return this.value.refresh == null
            }
        },
        from: {
            get() {
                var vm = this

                if (vm.isStatic)
                    return vm.filters.from

                return 0
            }
        },
        selected: {
            get() {
                var vm = this

                if (!vm.isStatic)
                    return vm.value.activeCount

                var count = 0

                vm.value.elements.forEach(function(element) {
                    if (!element.active) return

                    count++
                })

                return count
            }
        }
    },
    methods: {

        paginationSettings: function() {
            var vm = this
            return {
                refresh: function(from) {
                    vm.filters.from = from

                    if (vm.value.refresh != undefined)
                        vm.value.refresh(from)
                },
                threshold: 1,
                from: vm.filters.from,
                step: vm.filters.step,
                total: vm.value.total
            }
        },

        filter: function() {
            var vm = this
            return vm.value.elements.slice(vm.from, vm.from + vm.filters.step)
        },

        click: function(element) {
            var vm = this

            if (!element.clickable)
                return

            if (vm.value.click == undefined)
                return

            vm.value.click(element)
        },

        raiseFilterKeyUp: function() {
            var vm = this

            vm.filters.from = 0
            vm.value.filter(vm.filters.input)

            vm.$forceUpdate()
        }
    }
})

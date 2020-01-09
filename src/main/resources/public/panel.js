Vue.component('panel', {
    template: `
<nav class="panel">
    <p class="panel-heading">
        {{value.title}} ({{value.total}})
    </p>
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

    <a class="panel-block"
        v-for="element in filter()"
        @click="value.click != undefined && value.click(element)"
        v-bind:class="{ 'is-active': element.active != undefined && element.active }">

        <p class="panel-icon" v-if="element.icon">
            <i class="fas" aria-hidden="true" :class="element.icon"></i>
        </p>
        <span class="panel-icon" v-else-if="value.icon">
            <i class="fas" aria-hidden="true" :class="value.icon"></i>
        </span>

        <span class="tag is-primary" v-if="element.subLabel" style="margin-right: 0.75em">{{element.subLabel}}</span>

        {{element.label}}
    </a>

    </div>

    <hr />

    <pagination v-model="paginationSettings()" />

    <div class="panel-block">
        <button class="button is-link is-outlined is-fullwidth">
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
            filters: {
                input: '',
                panel: '',
                from: 0,
                step: 10
            }
        }
    },
    computed: {
        from: {
            get() {
                var vm = this

                if (vm.value.isStatic)
                    return vm.filters.from

                return 0
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

        raiseFilterKeyUp: function() {
            var vm = this

            vm.filters.from = 0
            vm.value.filter(vm.filters.input)

            vm.$forceUpdate()
        }
    }
})

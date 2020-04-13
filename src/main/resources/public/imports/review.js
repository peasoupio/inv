Vue.component('review', {
    template: `
<div>

    <p class="has-text-centered" v-if="!ready">The review is not ready yet. Make sure you installed something before.</p>
    <div>
        <div class="columns" >
            <div class="column is-3">
                <p class="title is-6">Information</p>
                <p>Base execution: {{getBaseRunTimestamp()}}</p>
                <p>Latest execution: {{getLatestTimestamp()}}</p>
            </div>
            <div class="column is-3">
                <p class="title is-6">
                    Stats
                </p>
                <div v-for="stat in stats">
                    <p>{{stat.name}}: {{statsValues[stat.value]}}
                        <a class="icon active" style="margin-right: 0.75em" @mouseover="stat.showHelp = true" @mouseleave="stat.showHelp = false">
                            <i class="fas fa-question-circle" aria-hidden="true"></i>
                        </a>
                    </p>
                    <div class="notification is-primary" v-show="stat.showHelp" style="position: absolute; z-index: 10">
                        <div class="content panel-help" v-html="stat.help"></div>
                    </div>
                </div>
            </div>
            <div class="column">
                <div class="field is-grouped is-grouped-right">
                    <div class="field">
                        <button
                            v-for="(obj, name) in filters"
                            @click="toggleFilter(obj)"
                            v-bind:class="{ 'is-link': obj.value}" class="button breath">
                            Hide {{obj.label}}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <p class="title is-5">
            Broadcast(s)
            <span v-show="loading">
                <i class="fas fa-spinner fa-pulse" aria-hidden="true"></i>
            </span>
        </p>

        <div class="field">
            <p class="control is-expanded has-icons-right">
                <input class="input" type="text" v-model="labelFilter" placeholder="Name and/or ID">
                <span class="icon is-small is-right"><i class="fas fa-search"></i></span>
            </p>
        </div>

        <hr />

        <p v-for="line in filter()">
            <span style="min-width: 2em; display: inline-block">{{line.owner.index + 1}}.</span>
            <span class="tag" v-if="line.state == '='">equals</span>
            <span class="tag is-info" v-if="line.state == '+'">added</span>
            <span class="tag is-warning" v-if="line.state == '-'">missing</span>
            <span class="tag is-danger" v-if="line.state == 'x'">removed</span>
            {{line.link.value}}
        </p>

        <p v-if="paginationFilters.total == 0" class="has-text-centered">Nothing to show</p>

        <div style="padding: 1em">
            <pagination v-model="paginationSettings" />
        </div>
    </div>
</div>
`,
    props: ['value', 'update'],
    data: function() {
        return {
            loading: false,
            ready: false,
            review: {},
            deltaLines: {},
            showStatHeaderHelp: false,
            stats: [
                { name: 'Equals', value: 'equals', showHelp: false, help: `
<ul>
<li>List how many statements are equals</li>
<li>Statement must be present in the <strong>base AND latest execution</strong></li>
 </ul>
 `},
                { name: 'Missing', value: 'missing', showHelp: false, help: `
<ul>
<li>List how many statements are missing <strong>but not removed</strong></li>
<li>Statement must be present in the <strong>base AND NOT in latest execution</strong></li>
</ul>
`},
                { name: 'Added', value: 'added', showHelp: false, help: `
<ul>
<li>List how many statements are added</li>
<li>Statement must be present in the <strong>latest execution AND NOT base</strong></li>
</ul>
`},
                { name: 'Removed', value: 'removed', showHelp: false, help: `
<ul>
<li>List how many statements are <strong>removed</strong></li>
<li>Remove occurs when a statement is missing and does not have an SCM associated with</li>
</ul>
`},
            ],
            labelFilter: '',
            filters: {
                hideEquals: {value: false, label: "equals" },
                hideMissing: {value: false, label: "missing" },
                hideAdded: {value: false, label: "added" },
                hideRemoved: {value: false, label: "removed" },
            },
            paginationFilters: {
                from: 0,
                step: 50,
                total: 0
            },

            statsValues: { }
        }
    },
    computed: {
        paginationSettings: {
            get() {
                var vm = this
                return {
                    refresh: function(from) {
                        vm.paginationFilters.from = from
                    },
                    threshold: 3,
                    from: vm.paginationFilters.from,
                    step: vm.paginationFilters.step,
                    total: vm.paginationFilters.total
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this
            var filtered = []

            Object.values(vm.deltaLines).forEach(function(line) {
                if (vm.filters.hideEquals.value && line.state == '=') return
                if (vm.filters.hideMissing.value && line.state == '-') return
                if (vm.filters.hideAdded.value && line.state == '+') return
                if (vm.filters.hideRemoved.value && line.state == 'x') return

                if (line.link.value.indexOf(vm.labelFilter) < 0) return


                filtered.push(line)
            })

            vm.paginationFilters.total = filtered.length

            if (vm.paginationFilters.total < vm.paginationFilters.from)
                vm.paginationFilters.from = 0

            return filtered
                    .sort((a, b) => a.owner.index - b.owner.index)
                    .slice(
                        vm.paginationFilters.from,
                        vm.paginationFilters.from + vm.paginationFilters.step)
        },
        getBaseRunTimestamp: function() {
            var vm = this
            return TimeAgo.inWords(vm.review.baseExecution)
        },
        getLatestTimestamp: function() {
            var vm = this
            return TimeAgo.inWords(vm.review.lastExecution)
        },
        getDelta: function() {
            var vm = this

            vm.loading = true

            axios.get(vm.value.api.links.review.default).then(response => {
                vm.review = response.data
                vm.statsValues = vm.review.stats

                response.data.lines.forEach(function(line) {
                    if (vm.deltaLines[line.link.value] == undefined)
                        vm.$set(vm.deltaLines, line.link.value, line)
                    else {
                        var existing = vm.deltaLines[line.link.value]
                        existing.state = line.state
                        existing.owner.index = line.owner.index
                    }
                })

                vm.paginationFilters.total = response.data.lines.length

                vm.loading = false
                vm.ready = true
            })
            .catch(function() {
                vm.loading = false
            })
        },
        toggleFilter: function(filter) {
            filter.value = !filter.value
            localStorage.reviewFilters = JSON.stringify(this.filters)
        }
    },
    mounted: function() {
        var vm = this

        if (localStorage.reviewFilters) {
            vm.filters = JSON.parse(localStorage.reviewFilters)
        }

        vm.getDelta()
    },
    watch: {
        update: function(newVal, oldVal) {
            var vm = this
            vm.getDelta()
        }
    }
})
Vue.component('review', {
    template: `
<div>
    <div class="columns">
        <div class="column is-3">
            <p class="title is-6">Information</p>
            <p>Base execution: {{getBaseRunTimestamp()}}</p>
            <p>Latest execution: {{getLatestTimestamp()}}</p>
        </div>
        <div class="column is-2">
            <p class="title is-6">Stats</p>
            <div v-for="stat in stats">
                <p>{{stat.name}}: {{statsValues[stat.value]}}
                    <a class="icon active" style="margin-right: 0.75em" @click="stat.showHelp = !stat.showHelp">
                        <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </a>
                </p>
                <div class="notification is-primary" v-show="stat.showHelp">
                    <div class="content panel-help" v-html="stat.help"></div>
                </div>
            </div>
        </div>
        <div class="column">
            <div class="field is-grouped is-grouped-right">
                <div class="field">
                    <button @click="filters.hideEquals = !filters.hideEquals" v-bind:class="{ 'is-link': filters.hideEquals}" class="button">
                        Hide equals
                    </button>
                    <button @click="filters.hideMissing = !filters.hideMissing" v-bind:class="{ 'is-link': filters.hideMissing}" class="button">
                        Hide missing
                    </button>
                    <button @click="filters.hideAdded = !filters.hideAdded" v-bind:class="{ 'is-link': filters.hideAdded}" class="button">
                        Hide added
                    </button>
                </div>
            </div>
        </div>
    </div>

    <hr />

    <div style="overflow-y: scroll; height: 600px; width: 100%">
        <pre
            v-for="deltaLine in filter()"
            v-bind:class="{ 'missing' : deltaLine.state === '-', 'added' : deltaLine.state === '+'}">
{{deltaLine.link.value}}</pre>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            review: {},
            deltaLines: [],
            stats: [
                { name: 'Equals', value: 'equals', showHelp: false, help: `
<ul>
<li>List how many statements are equals</li>
<li>Include only <strong>broadcast</strong> statements</li>
<li>Statement must be present in the <strong>base AND latest execution</strong>` },
                { name: 'Missing', value: 'missing', showHelp: false, help: '2' },
                { name: 'Added', value: 'added', showHelp: false, help: '3' }
            ],
            filters: {
                hideEquals: false,
                hideMissing: false,
                hideAdded: false
            },

            statsValues: {
                equals: 0,
                missing: 0,
                added: 0
            }
        }
    },
    methods: {

        filter: function() {
            var vm = this
            var filtered = []

            vm.deltaLines.forEach(function(deltaLine) {
                if (vm.filters.hideEquals && deltaLine.state == '=') return
                if (vm.filters.hideMissing && deltaLine.state == '-') return
                if (vm.filters.hideAdded && deltaLine.state == '+') return

                filtered.push(deltaLine)
            })

            return filtered.slice(0, 100)
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

            axios.get(vm.value.api.links.review.default).then(response => {
                vm.review = response.data
                vm.deltaLines = response.data.lines.sort((a, b) => a.index - b.index)

                vm.deltaLines.forEach(function(deltaLine) {

                    switch(deltaLine.state) {
                        case '+':
                            vm.statsValues.added++
                            break

                        case '-':
                            vm.statsValues.missing++
                            break

                        case '=':
                            vm.statsValues.equals++
                            break
                    }

                })

            })
        }
    },
    created: function() {
        this.getDelta()
    }
})
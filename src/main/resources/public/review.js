Vue.component('review', {
    template: `


<div>


    <p class="title is-1">Review: </p>

    <p>Missing: {{stats.missing}}</p>
    <p>Added: {{stats.added}}</p>
    <p>Equals: {{stats.equals}}</p>

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
            deltaLines: [],
            filters: {
                hideEquals: false,
                hideMissing: false,
                hideAdded: false
            },

            stats: {
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

        getDelta: function() {
            var vm = this

            axios.get(vm.value.api.links.review.default).then(response => {
                vm.deltaLines = response.data.lines.sort((a, b) => a.index - b.index)

                vm.deltaLines.forEach(function(deltaLine) {

                    switch(deltaLine.state) {
                        case '+':
                            vm.stats.added++
                            break

                        case '-':
                            vm.stats.missing++
                            break

                        case '=':
                            vm.stats.equals++
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


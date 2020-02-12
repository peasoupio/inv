Vue.component('review', {
    template: `
<div>
    <div class="columns">
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
                <div class="notification is-primary" v-show="stat.showHelp" style="position: absolute">
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

    <p class="title is-4">
        Broadcast(s)
        <span v-show="loading">
            <i class="fas fa-spinner fa-pulse" aria-hidden="true"></i>
        </span>
    </p>

    <hr />

    <div class="output">
        <div id="linesContainer" ref="linesContainer"></div>
        <div class="anchor"></div>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            loading: false,
            review: {},
            deltaLines: [],
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
            filters: {
                hideEquals: {value: false, label: "equals" },
                hideMissing: {value: false, label: "missing" },
                hideAdded: {value: false, label: "added" },
                hideRemoved: {value: false, label: "removed" },
            },

            statsValues: { }
        }
    },
    methods: {

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
                vm.statsValues = vm.review.stats
                vm.deltaLines = response.data.lines.sort((a, b) => a.index - b.index)

                vm.drawLines()
            })
        },
        drawLines: function() {
            var vm = this
            var filtered = []
            var linesContainer = this.$refs.linesContainer

            vm.loading = true
            vm.clearLines(linesContainer)

            vm.deltaLines.forEach(function(deltaLine) {
                if (vm.filters.hideEquals.value && deltaLine.state == '=') return
                if (vm.filters.hideMissing.value && deltaLine.state == '-') return
                if (vm.filters.hideAdded.value && deltaLine.state == '+') return
                if (vm.filters.hideRemoved.value && deltaLine.state == 'x') return

                filtered.push(deltaLine)
            })

            var temp = 0
            var interval = setInterval(function() {
                temp = 0
                while(temp < 128 && filtered.length > 0) {
                    temp++

                    var line = filtered.shift()
                    var type = ""
                    switch(line.state) {
                        case "=": type = "equals"; break
                        case "+": type = "added"; break
                        case "-": type = "missing"; break
                        case "x": type = "removed"; break
                    }

                    vm.appendLine(linesContainer, type, line.link.value)
                }

                if (filtered.length == 0) {
                    clearInterval(interval)
                    vm.loading = false
                }
            }, 125)

            return filtered
        },
        clearLines: function(linesContainer) {
            var child = linesContainer.lastElementChild;
            while (child) {
                linesContainer.removeChild(child);
                child = linesContainer.lastElementChild;
            }
        },
        appendLine: function(linesContainer, type, line) {
            var paragraph = document.createElement("P")

            var tag = document.createElement("SPAN")
            tag.className = "tag"
            switch(type) {
                case "equals": tag.className += ""; break
                case "added": tag.className += " is-info"; break
                case "missing": tag.className += " is-warning"; break
                case "removed": tag.className += " is-danger"; break
            }
            tag.appendChild(document.createTextNode(type))

            var text = document.createElement("SPAN")
            text.className = "link"
            text.appendChild(document.createTextNode(line))

            paragraph.appendChild(tag)
            paragraph.appendChild(text)

            linesContainer.appendChild(paragraph)
        },
        toggleFilter: function(filter) {
            filter.value = !filter.value
            this.drawLines()
        }
    },
    mounted: function() {
        this.getDelta()
    }
})
Vue.component('choose', {
    template: `
<div>
    <!--
    <div class="tabs">
        <ul>
            <li><a>Simple ({{value.invs.count}}/{{value.invs.total}})</a></li>
            <li><a>Summary ({{ + value.invs.requiredByAssociation}}/{{value.invs.total}})</a></li>
        </ul>
    </div>
    -->

    <div class="columns">
        <div class="column is-2">
            <tab-tiles v-model="tabTilesSettings" />
        </div>

        <div class="column" v-if="currentTab.template">
            <component v-bind:is="currentTab.template" v-model="value" />
        </div>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentTab: {}
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'Simple', description: 'Select from a simple view of INVs', template: 'choose-select-simple', isWhat: 'link'},
                        { label: 'Complex', description: 'Select from a more detailed view of INVs', template: 'choose-select-complex', isWhat: 'danger'},
                        { label: 'Resume', description: 'A detailed view of your current selections', template: 'choose-summary', isWhat: 'primary'},
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()
                    }
                }
            }
        }
    }
})

Vue.component('choose-select-simple', {
    template: `
<div class="columns">

    <div class="column is-4">
        <panel v-model="ownersSettings" v-if="owners.length > 0"/>
    </div>

    <div class="column is-6" v-if="ids.length > 0">
        <panel v-model="idsSettings" />
    </div>


</div>
`,
    props: ['value'],
    data: function() {
        return {
            owners: [],
            ids: [],
            idsTotal: 0,
            filters: {
                selected: false,
                required: false,
                owner: '',
                id: '',
                step: 10,
                from: 0
            }
        }
    },
    computed: {
        ownersSettings: {
            get() {
                var vm = this

                var owners  = []
                vm.owners.forEach(function(owner) {
                    owners.push({
                        label: owner.owner,
                        clickable: true
                     })
                })

                owners.sort(compareValues('label'))

                var settings = {
                    isStatic: true,
                    clickable: true,
                    title: "Owners",
                    icon: 'fa-file-signature',
                    total: owners.length,
                    elements: owners
                }

                settings.click = function(element) {
                    vm.filters.owner = element.label
                    vm.searchNodes()
                }

                settings.filter = function(word) {
                    if (word == '') {
                        settings.elements = owners
                        settings.total = owners.length
                    }

                    var filtered = owners.filter(function(owner) {
                        return owner.label.indexOf(word) > -1
                    })

                    filtered.sort(compareValues('label'))

                    settings.elements = filtered
                    settings.total = filtered.length
                }

                return settings
            }
        },
        idsSettings: {
            get() {
                var vm = this

                vm.ids.sort(compareValues('label'))

                var settings = {
                    isStatic: false,
                    clickable: true,
                    title: "Ids of: " + vm.filters.owner,
                    icon: 'fa-fingerprint',
                    total: vm.idsTotal,
                    elements: vm.ids
                }

                settings.click = function(element) {
                    if (element.sending)
                        return

                    element.icon = 'fa-spinner fa-pulse'
                    element.sending = true
                    vm.doSelect(element.inv)
                }

                settings.refresh = function(from) {
                    vm.filters.from = from
                    vm.searchNodes()
                }

                settings.filter = function(word) {
                    vm.filters.id = word
                    vm.searchNodes()
                }

                return settings
            }
        }
    },
    methods: {

        searchNodes: function() {
            var vm = this

            var ids =  []

            axios.post(vm.value.api.links.run.search, vm.filters).then(response => {

                response.data.nodes.forEach(function(inv) {
                    ids.push({
                        icon: '',
                        label: inv.id,
                        subLabel: inv.name,
                        active: inv.selected || inv.required,
                        clickable: !inv.required,
                        inv: inv
                    })
                })

                vm.idsTotal = response.data.count
                vm.ids = ids
            })
        },


        doSelect: function(inv) {
            var vm = this

            if (!inv.selected) {
                axios.post(inv.links.stage, vm.filters).then(response => {
                    vm.searchNodes()
                })
            } else {
                axios.post(inv.links.unstage, vm.filters).then(response => {
                    vm.searchNodes()
                })
            }
        },


    },
    created: function() {
        var vm = this

        axios.get(vm.value.api.links.run.owners).then(response => {
            vm.owners = response.data
        })
    }

})
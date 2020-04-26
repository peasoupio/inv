Vue.component('choose-tags', {
    template: `
<div>

    <div v-if="!tags.length">
        No Tags available yet...
    </div>
    <div v-else>
        <div class="columns">
            <div class="column is-half">
                <p class="title is-4">Refine your selection :</p>
                <aside class="menu">
                  <ul class="menu-list">
                    <li v-for="tag in tags">
                        <a>{{tag.label}}</a>
                        <ul>
                            <li v-for="subtag in tag.subTags">
                                <a @click="previewTag = subtag" v-bind:class="{ 'is-active' : previewTag == subtag }">
                                    {{subtag.label}}
                                    <span class="tag is-info is-pulled-right" v-if="subtag.selectedCount > 0" style="margin-left: 1em">{{subtag.selectedCount}} selected</span>
                                    <span class="tag is-primary is-pulled-right" v-if="subtag.requiredCount > 0">{{subtag.requiredCount}} required</span>
                                </a>
                                <ul v-if="previewTag == subtag">
                                    <li v-for="inv in subtag.invs">
                                        <a @click="previewInv = inv" v-bind:class="{ 'is-active' : previewInv == inv }">
                                            {{inv.label}}
                                            <span class="tag is-info is-pulled-right" v-if="inv.selected > 0" style="margin-left: 1em">selected</span>
                                            <span class="tag is-primary is-pulled-right" v-if="inv.required > 0">required</span>
                                        </a>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                  </ul>
                </aside>
            </div>
            <div class="column is-half">

                <div v-if="!previewTag && !previewInv"><p>Nothing selected yet. Click on an element from menu to the left.</p></div>
                <div class="notification content" v-if="previewTag">
                    <button class="delete" @click="previewTag = null"></button>
                    <p class="title is-4">Tag: <strong>{{previewTag.parent.label}}:<strong>{{previewTag.label}}</strong></p>
                    <p>INV's: <strong>{{previewTag.invs.length}}</strong></p>
                    <p>Selected: <strong>{{previewTag.selectedCount}}</strong>/{{previewTag.invs.length}}</p>
                    <p>Required: <strong>{{previewTag.requiredCount}}</strong>/{{previewTag.invs.length}}</p>
                    <div class="field is-grouped">
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" @click="unstageAll()">
                          <span>Unstage</span>
                        </button>
                      </p>
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" @click="stageAll()">
                          <span>Stage</span>
                        </button>
                      </p>
                    </div>
                </div>
                <div class="notification content" v-if="previewInv">
                    <button class="delete" @click="previewInv = null"></button>
                    <p class="title is-4">INV: <strong>{{previewInv.label}}</strong></p>
                    <p>Selected :
                        <span class="tag is-info" v-if="previewInv.selected > 0">selected</span>
                        <span class="tag" v-else>not selected</span>
                    </p>
                    <p>Required by: <strong>{{previewInv.required}}</strong></p>

                    <div class="field is-grouped">
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" :disabled="previewInv.required > 0 || previewInv.selected == 0" @click="unstage()">
                          <span>Unstage</span>
                        </button>
                      </p>
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" :disabled="previewInv.required > 0 || previewInv.selected > 0" @click="stage()">
                          <span>Stage</span>
                        </button>
                      </p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            tags: [],
            previewTag: null,
            previewInv: null
        }
    },
    methods: {
        searchTags: function() {
            var vm = this
            vm.tags = []
            vm.previewTag = null
            vm.previewInv = null

            axios.get(vm.value.api.links.run.tags).then(response => {
                var tags = response.data.tags
                tags.forEach(tag => {
                    // Process subtags
                    var subTags = []
                    tag.subTags.forEach(subTag => {

                        var selectedCount = 0
                        var requiredCount = 0
                        subTag.invs.forEach(inv => {
                            if (inv.selected > 0)
                                selectedCount++
                            if (inv.required)
                                requiredCount++
                        })

                        subTag.invs.sort(compareValues('label'))
                        subTags.push({
                            label: subTag.label,
                            parent: tag,
                            links: subTag.links,
                            active: false,
                            invs: subTag.invs,
                            selectedCount: selectedCount,
                            requiredCount: requiredCount
                        })

                    })
                    subTags.sort(compareValues('label'))

                    var tagObj = {
                        label: tag.label,
                        links: tag.links,
                        subTags: subTags
                    }

                    vm.tags.push(tagObj)
                })

                vm.tags.sort(compareValues('label'))
            })
        },
        stage: function(tag) {
            var vm = this

            if (!vm.previewInv)
                return

            axios.post(vm.previewInv.links.stage).then(response => {
                vm.searchTags()
            })
        },
        unstage: function(tag) {
            var vm = this

            if (!vm.previewInv)
                return

            axios.post(vm.previewInv.links.unstage).then(response => {
                vm.searchTags()
            })
        },
        stageAll: function() {
            var vm = this

            if (!vm.previewTag)
                return

            axios.post(vm.previewTag.links.stageAll).then(response => {
                vm.searchTags()
            })
        },
        unstageAll: function() {
            var vm = this

            if (!vm.previewTag)
                return

            axios.post(vm.previewTag.links.unstageAll).then(response => {
                vm.searchTags()
            })
        }
    },
    created: function() {
        this.searchTags()
    }
})
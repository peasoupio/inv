Vue.component('choose-tags', {
    template: `
<div>

    <div v-if="!tags.length">
        No Tags available yet...
    </div>
    <div v-else>
        <div class="columns">
            <div class="column is-half">
                <p class="title is-4">Refine your selection:</p>
                <aside class="menu">
                  <ul class="menu-list">
                    <li v-for="tag in tags">
                        <a>{{tag.label}}</a>
                        <ul>
                            <li v-for="subtag in tag.subTags">
                                <a @click="stageTag(subtag)" v-bind:class="{ 'is-active' : previewTag == subtag }">
                                    {{subtag.label}}
                                    <span class="tag is-primary is-pulled-right" v-if="subtag.required > 0" style="margin-left: 1em">{{subtag.required}} required</span>
                                    <span class="tag is-info is-pulled-right" v-if="subtag.staged > 0" >{{subtag.staged}} staged</span>

                                </a>
                                <ul v-if="previewTag == subtag">
                                    <li v-for="inv in subtag.invs">
                                        <a @click="stageInv(inv)" v-bind:class="{ 'is-active' : previewInv == inv }">
                                            {{inv.label}}
                                            <span class="tag is-info is-pulled-right" v-if="inv.staged > 0" style="margin-left: 1em">staged</span>
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
                <p class="title is-4">Current selection:</p>
                <div v-if="!previewTag && !previewInv"><p>Nothing selected yet. Click on an element from menu to the left.</p></div>
                <div class="notification content" v-if="previewTag">
                    <button class="delete" @click="previewTag = null"></button>
                    <p class="title is-4">Tag: <strong>{{previewTag.parent.label}}:<strong>{{previewTag.label}}</strong></p>
                    <p>INV's: <strong>{{previewTag.invs.length}}</strong></p>
                    <p>Staged: <strong>{{previewTag.staged}}</strong>/{{previewTag.invs.length}}</p>
                    <p>Required: <strong>{{previewTag.required}}</strong>/{{previewTag.invs.length}}</p>
                    <div class="field is-grouped" v-if="canStageAll()">
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
                    <p>Status:
                        <span class="tag is-info" v-if="previewInv.staged > 0">staged</span>
                        <span class="tag" v-else>not staged</span>
                    </p>
                    <p>Required by: <strong>{{previewInv.required}}</strong></p>

                    <div class="field is-grouped" v-if="canStage()">
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" :disabled="previewInv.required > 0 || previewInv.staged == 0" @click="unstage()">
                          <span>Unstage</span>
                        </button>
                      </p>
                      <p class="control is-expanded">
                        <button class="button is-fullwidth" :disabled="previewInv.required > 0 || previewInv.staged > 0" @click="stage()">
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
            const vm = this
            vm.tags = []
            vm.previewTag = null
            vm.previewInv = null

            axios.get(vm.value.api.links.run.tags).then(response => {
                const tags = response.data.tags
                tags.forEach(tag => {

                    // Process sub-tags
                    const subTags = []
                    tag.subTags.forEach(subTag => {

                        let stagedCount = 0
                        let requiredCount = 0

                        subTag.invs.forEach(inv => {
                            if (inv.staged > 0)
                                stagedCount++
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
                            staged: stagedCount,
                            required: requiredCount
                        })

                    })
                    subTags.sort(compareValues('label'))

                    const tagObj = {
                        label: tag.label,
                        links: tag.links,
                        subTags: subTags
                    }

                    vm.tags.push(tagObj)
                })

                vm.tags.sort(compareValues('label'))
            })
        },

        stageTag: function(tag) {
            const vm = this

            if (vm.previewTag === tag)
                vm.previewTag = null
            else
                vm.previewTag = tag
        },

        stageInv: function(inv) {
            const vm = this

            if (vm.previewInv === inv)
                vm.previewInv = null
            else
                vm.previewInv = inv
        },

        stage: function() {
            const vm = this

            if (!vm.previewInv)
                return

            axios.post(vm.previewInv.links.stage).then(() => {
                vm.searchTags()
            }).catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>stage INV</strong>!`)
            })

        },
        unstage: function() {
            const vm = this

            if (!vm.previewInv)
                return

            axios.post(vm.previewInv.links.unstage).then(() => {
                vm.searchTags()
            }).catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>unstage INV</strong>!`)
            })

        },
        stageAll: function() {
            const vm = this

            if (!vm.previewTag)
                return

            axios.post(vm.previewTag.links.stageAll).then(() => {
                vm.searchTags()
            }).catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>stage INVs</strong>!`)
            })
        },
        unstageAll: function() {
            const vm = this

            if (!vm.previewTag)
                return

            axios.post(vm.previewTag.links.unstageAll).then(() => {
                vm.searchTags()
            }).catch(() => {
                vm.$bus.$emit('toast', `error:Failed to <strong>unstage INVs</strong>!`)
            })
        },
        canStage: function() {
            if (!this.previewInv) return false

            return this.previewInv.links.stage
        },
        canStageAll: function() {
            if (!this.previewTag) return false

            return this.previewTag.links.stageAll
        }
    },
    created: function() {
        this.searchTags()
    }
})
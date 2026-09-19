/*
 * @Author: 朽木白
 * @LastEditors: 1547702880@@qq.com
 * @Description: 播放器状态
 */


import { defineStore } from "pinia";

interface Ids {
  albumId: number,
  trackId: number
}

export const usePlayerStore = defineStore("player", {
  state: () => ({
    playing: false,
    albumId: 0,
    trackId: 0
  }),
  actions: {
    changePlayStatus(status: boolean) {
      this.playing = status
    },
    setId(state: Ids) {
      const { albumId, trackId } = state
      this.albumId = albumId
      this.trackId = trackId
    }
  },
  persist: {
    enabled: true,
  },
});

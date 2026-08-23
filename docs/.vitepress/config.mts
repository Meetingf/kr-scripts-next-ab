import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  base: '/kr-scripts-next/',
  title: "Kr Scripts Next",
  description: "Make Apps with Xml-UI and Shell",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/Intro.md' }
    ],

    sidebar: [
      {
        text: 'Introduction',
        items: [
          { text: 'What is KrScript?', link: '/Intro.md' },
          { text: 'Migration from Kr-Scripts', link: '/Migration.md' }
        ]
      },
      {
        text: 'Configuration',
        items: [
          { text: 'Config', link: '/Config.md' },
          { text: 'Navigation', link: '/Navigation.md' }
        ]
      },
      {
        text: 'Feature Node',
        items: [
          { text: 'Action', link: '/Action.md' },
          { text: 'Switch', link: '/Switch.md' },
          { text: 'Picker', link: '/Picker.md' },
          { text: 'Page', link: '/Page.md' },
          { text: 'Common', link: '/NodeCommon.md' },
        ]
      },
      {
        text: 'Appearance Node',
        items: [
          { text: 'Text', link: '/Text.md' },
          { text: 'Group', link: '/Group.md' }
        ]
      },
      {
        text: 'Other',
        items: [
          { text: 'Scripts', link: '/Script.md' },
          { text: 'Resources', link: '/Resource.md' },
          { text: 'Compatibility', link: '/Compatibility.md' },
          { text: 'Web Engine', link: '/WebEngine.md' }
        ]
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/buylan01/kr-scripts-next' }
    ],

    footer: {
      message: '本文档基于 <a href="https://github.com/helloklf/kr-scripts" target="_blank">kr-scripts</a>（GPL v3）修改整理。',
      copyright: '原始文档 © kr-scripts 原作者 | 修改与站点构建 © 2026 buylan'
    }
  }
})

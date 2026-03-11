# 📌 Musubi Project (Language Exchange Platform)

> **💡 案内 (Notice)**
> 本リポジトリは、5名で共同開発したチームプロジェクト「Musubi」のうち、自身が専任で設計・実装を担当した**「リアルタイムチャットおよびAI機能」**を中心にまとめたポートフォリオ用リポジトリです。
> （※チーム全体のオリジナルリポジトリは[こちら](https://github.com/SMARTCLOUDIT48/Backend)）

## 📖 プロジェクト概要
- **プロジェクト説明**: 言語交換プラットフォームにおけるリアルタイムチャットおよびAIアシスタント機能の開発
- **開発期間**: 2026.01.19 ~ 2026.03.05（約1.5ヶ月）
- **チーム構成**: 計5名（自身の担当: チャットおよびAI機能のフルスタック開発）
- **技術スタック**: Java (Spring Boot), Spring WebSocket, MySQL, Redis, HTML/CSS/JavaScript, Grok AI
- **担当役割**: バックエンド & フロントエンドエンジニア（チャット・AI機能専任）
- **🌐 デプロイサイト**: [後日アップデート予定](ここにURLを入力してください)
- **🔑 テスト用管理者アカウント**: ID: `admin` / PW: `admin`

### 💡 主な担当業務と成果
* **WebSocketを活用したリアルタイムチャットシステムの実装**
  * WebSocket通信を利用して遅延のないスムーズなメッセージ送受信環境を構築し、チャットルームの動的生成およびチャット履歴の永続化（DB保存）を実装しました。
* **Grok AIを統合したインタラクティブな言語学習支援機能の構築**
  * チャットサーバーとAI APIを連携させ、AIによる自動応答、多言語のリアルタイム翻訳、ユーザー入力文のスペルチェック（文章校正）機能をチャット内に組み込みました。
* **音声技術（TTS・録音）によるマルチメディア体験の提供**
  * テキストメッセージを自然な音声で読み上げるAI音声生成（Text-to-Speech）と、Web APIを活用したブラウザ上での音声録音・送信機能を構築しました。

---

## 🛠 Tech Stack

### Backend
<p>
<img src="https://img.shields.io/badge/Java-007396?style=flat&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=flat&logo=springboot&logoColor=white"/>
<img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=flat&logo=thymeleaf&logoColor=white"/>
</p>

### Frontend
<p>
<img src="https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white"/>
<img src="https://img.shields.io/badge/CSS3-1572B6?style=flat&logo=css3&logoColor=white"/>
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black"/>
</p>

### Database
<p>
<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white"/>
<img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white"/>
</p>

### AI
<p>
<img src="https://img.shields.io/badge/Grok%20AI-000000?style=flat&logo=x&logoColor=white"/>
</p>

### Development & Version Control
<p>
<img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=flat&logo=intellijidea&logoColor=white"/>
<img src="https://img.shields.io/badge/VSCode-007ACC?style=flat&logo=visualstudiocode&logoColor=white"/>
<img src="https://img.shields.io/badge/Git-F05032?style=flat&logo=git&logoColor=white"/>
<img src="https://img.shields.io/badge/GitHub-181717?style=flat&logo=github&logoColor=white"/>
<img src="https://img.shields.io/badge/Notion-000000?style=flat&logo=notion&logoColor=white"/>
</p>

---

## 🚀 担当機能の詳細 (Assigned Features)

**チャットおよびAI機能のフルスタック開発を専任**

* **リアルタイムチャット機能**
  * WebSocketベースのリアルタイム通信の実装
  * チャットルームの動的作成およびライフサイクル管理
  * チャットメッセージのDB保存および永続化処理
  * 画像や音声などのファイル送信機能
* **AIを活用したインタラクティブ機能（Grok AI連携）**
  * **AI自動応答**: ユーザーの入力内容を分析し、AIが適切な回答を自動生成してチャットに表示（サーバーサイドからのAPI非同期呼び出し）
  * **AIスペルチェック**: 外国語学習用のリアルタイム文章校正・文法チェック機能
  * **AI翻訳**: 多言語間のコミュニケーションを支援するリアルタイム翻訳機能
* **音声インターフェース**
  * **AI音声生成 (TTS)**: チャットのテキストを自然な音声に変換するText-to-Speech機能
  * **音声録音機能**: ブラウザ上での音声録音およびメッセージ送信機能の実装

---

## 📸 担当機能プレビュー (Project Preview)

### 💬 チャット基本画面（リアルタイム通信）
<img src="src/main/resources/static/images/readmeimages/chat.png" width="500">

### 🤖 チャットの主要機能（AI翻訳・文章校正・AI自動応答など）
<img src="src/main/resources/static/images/readmeimages/chatdetail.png" width="500">

<br/>

<img src="src/main/resources/static/images/readmeimages/chatdetail2.png" width="500">

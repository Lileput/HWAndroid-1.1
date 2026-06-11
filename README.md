# NMedia (NeWork)

Клиентская часть учебного проекта **NMedia** — социальная сеть с постами, событиями, профилями пользователей и push-уведомлениями. Приложение работает с REST API Netology, кэширует данные локально и поддерживает офлайн-просмотр ленты.

## Функциональность

### Посты
- Лента с пагинацией (Paging 3 + Room)
- Создание и редактирование постов
- Вложения: фото, видео, аудио
- Отметки пользователей и геолокация на карте (Yandex MapKit)
- Лайки, репосты, просмотр лайкнувших
- Воспроизведение видео и аудио во встроенном плеере (Media3 / ExoPlayer)

### События
- Лента событий (онлайн / офлайн)
- Создание и редактирование событий
- Участие в событии, лайки, список участников
- Вложения и аудио с кнопкой воспроизведения в карточке

### Пользователи и профиль
- Список пользователей
- Профиль: аватар, стена, места работы
- Регистрация и авторизация
- Редактирование аватара

### Прочее
- Push-уведомления (Firebase Cloud Messaging)
- Загрузка изображений через Glide + ImageKit CDN
- Поделиться постом / событием

## Технологии

| Категория | Стек |
|-----------|------|
| Язык        | Kotlin |
| UI          | View Binding, Material 3, Navigation Component |
| Архитектура | MVVM, Hilt, Coroutines, LiveData |
| Сеть        | Retrofit, OkHttp |
| БД          | Room, Paging |
| Медиа       | Glide, ExoPlayer (Media3), ImagePicker, uCrop |
| Карты       | Yandex MapKit |
| Push        | Firebase Messaging |

## Требования

- Android Studio (рекомендуется последняя стабильная версия)
- JDK 21
- minSdk 26, targetSdk 36
- Устройство или эмулятор с Google Play (для FCM — опционально)

## Настройка и запуск

1. Клонируйте репозиторий.
2. Скопируйте `local.properties.example` в `local.properties`.
3. Укажите ключи:
   - `NMEDIA_API_KEY` — персональный Api-Key из личного кабинета Netology
   - `MAPKIT_API_KEY` — ключ Yandex MapKit Mobile SDK (пакет `ru.netology.nmedia`)
4. Для push-уведомлений добавьте `google-services.json` в папку `app/` (из Firebase Console).
5. Выполните **Sync Project with Gradle Files** и **Run**.

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
NMEDIA_API_KEY=your-api-key
MAPKIT_API_KEY=your-mapkit-key
```

## Скриншоты

---

### 1. Лента постов

![Лента постов](screenshots/01_feed.png)

---

### 2. Создание поста

![Создание поста](screenshots/02_new_post.png)

---

### 3. Пост с медиа и воспроизведение

![Пост с медиа](screenshots/03_post_media.png)

![Встроенный плеер](screenshots/03b_media_player.png)

---

### 4. Лента событий

![Лента событий](screenshots/04_events.png)

---

### 5. Создание события

![Создание события](screenshots/05_new_event.png)

---

### 6. Список пользователей

![Список пользователей](screenshots/06_users.png)

---

### 7. Профиль пользователя

![Профиль](screenshots/07_profile.png)

---

### 8. Авторизация

![Вход в аккаунт](screenshots/08_sign_in.png)

---

### 9. Карта в посте (опционально)

![Карта](screenshots/09_map.png)

---

## Структура проекта (кратко)

```
app/src/main/java/ru/netology/nmedia/
├── activity/       # Экраны и фрагменты
├── adapter/        # RecyclerView / Paging адаптеры
├── api/            # Retrofit, OkHttp
├── auth/           # Авторизация, токен
├── db/             # Room
├── repository/     # Репозитории и RemoteMediator
├── viewModel/      # ViewModel
└── util/           # Загрузка медиа, карты, форматирование
```
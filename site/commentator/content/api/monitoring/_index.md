---
title: Monitoring
weight: 30
disableToc: false
---

## Healthz

- **GET** `/healthz`

---

```
curl localhost:8787/healthz

{"message":"ok"}
```

## Metrics

You can [configure](howto/configuration/) Commentator to expose metrics using the Prometheus format on a dedicated port.

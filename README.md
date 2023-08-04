# Generating Protocol Definitions

## Java
```shell
protoc --java_out=src\main\generated .\src\Messages.proto
```

## TypeScript

```shell
npx protoc --ts_out .\src\node .\src\Messages.proto
```

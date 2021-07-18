Invoke-Expression -Command (Get-ECRLoginCommand -Region eu-central-1).Command
docker tag facecheck/facecheck-server:latest 217807359435.dkr.ecr.eu-central-1.amazonaws.com/facecheck/facecheck-server:latest
docker push 217807359435.dkr.ecr.eu-central-1.amazonaws.com/facecheck/facecheck-server:latest
Pause
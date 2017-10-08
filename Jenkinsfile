node {
    stage 'Checkout'
    checkout scm
}
 
@Library('dwt-jenkins')  
import com.ge.dwt.jenkins.PipelineBuilder  
def pb = new PipelineBuilder()  
pb.generateProdDeployPipeline(     
	jenkins: 'low-risk-vpc',     
	artifactid: 'myapp',     
	fileext: 'noarch.rpm',     

) 


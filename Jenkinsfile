
@Library('dwt-jenkins')  
import com.ge.dwt.jenkins.PipelineBuilder  
import com.ge.dwt.jenkins.Notifications ()  

def pb = new PipelineBuilder()  
pb.generateProdDeployPipeline(     
	jenkins: 'master',     
	artifactid: 'MBEFC97',     
	fileext: 'rpm',     
         appslack: '#Techsol-Devops',
         appapprover: 'atul',
) 


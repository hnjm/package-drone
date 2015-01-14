<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib tagdir="/WEB-INF/tags" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<h:main title="Attach Artifact to '${fn:escapeXml(artifact.name) }'">

<ul class="button-bar">
	<li><a class="btn btn-default" href="view">Back</a></li>
</ul>

<p>
Artifact Id: ${fn:escapeXml(artifact.id) }
</p>

<form method="post" action="" enctype="multipart/form-data" class="form-horizontal">
    <fieldset>
        <legend>Attach artifact</legend>
        
        <div class="form-group">
            <label for="name" class="col-sm-2 control-label">File Name</label>
            <div class="col-sm-10">
                <input type="text" id="name" name="name" class="form-control"/>
            </div>
        </div>
        
        <div class="form-group">
	        <label for="file" class="col-sm-2 control-label">File</label>
	        <div class="col-sm-10">
	           <input type="file" id="file" name="file"/>
	        </div>
        </div>
        
        <button type="submit" class="btn btn-primary">Upload</button>
    </fieldset>
</form>

</h:main>